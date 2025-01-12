package org.cqfn.diktat.ruleset.rules.chapter3

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.BLANK_LINE_BETWEEN_PROPERTIES
import org.cqfn.diktat.ruleset.constants.Warnings.WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.*

import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CLASS_INITIALIZER
import com.pinterest.ktlint.core.ast.ElementType.COMPANION_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.CONST_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.LATEINIT_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.core.ast.ElementType.PRIVATE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * Rule that checks order of declarations inside classes, interfaces and objects.
 */
class ClassLikeStructuresOrderRule(configRules: List<RulesConfig>) : DiktatRule(
    NAME_ID,
    configRules,
    listOf(BLANK_LINE_BETWEEN_PROPERTIES, WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES)
) {
    override fun logic(node: ASTNode) {
        if (node.elementType == CLASS_BODY) {
            checkDeclarationsOrderInClass(node)
        } else if (node.elementType == PROPERTY) {
            checkNewLinesBeforeProperty(node)
        }
    }

    private fun checkDeclarationsOrderInClass(node: ASTNode) {
        val allProperties = AllProperties.fromClassBody(node)
        val initBlocks = node.getAllChildrenWithType(CLASS_INITIALIZER)
        val constructors = node.getAllChildrenWithType(SECONDARY_CONSTRUCTOR)
        val methods = node.getAllChildrenWithType(FUN)
        val (usedClasses, unusedClasses) = node.getUsedAndUnusedClasses()
        val (companionObject, objects) = node.getAllChildrenWithType(OBJECT_DECLARATION)
            .partition { it.hasModifier(COMPANION_KEYWORD) }
        val blocks = Blocks(
            (node.psi as KtClassBody).enumEntries.map { it.node },
            allProperties, objects, initBlocks, constructors,
            methods, usedClasses, companionObject, unusedClasses
        )
            .allBlockFlattened()
            .map { astNode ->
                listOf(astNode) +
                        astNode.siblings(false)
                            .takeWhile { it.elementType == WHITE_SPACE || it.isPartOfComment() }
                            .toList()
            }

        node.checkAndReorderBlocks(blocks)
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun checkNewLinesBeforeProperty(node: ASTNode) {
        // checking only top-level and class-level properties
        if (node.treeParent.elementType != CLASS_BODY) {
            return
        }

        val previousProperty = node.prevSibling { it.elementType == PROPERTY } ?: return

        val hasCommentBefore = node
            .findChildByType(TokenSet.create(KDOC, EOL_COMMENT, BLOCK_COMMENT))
            ?.isFollowedByNewline()
            ?: false
        val hasAnnotationsBefore = (node.psi as KtProperty)
            .annotationEntries
            .any { it.node.isFollowedByNewline() }
        val hasCustomAccessors = (node.psi as KtProperty).accessors.isNotEmpty() ||
                (previousProperty.psi as KtProperty).accessors.isNotEmpty()

        val whiteSpaceBefore = previousProperty.nextSibling { it.elementType == WHITE_SPACE } ?: return
        val isBlankLineRequired = hasCommentBefore || hasAnnotationsBefore || hasCustomAccessors
        val numRequiredNewLines = 1 + (if (isBlankLineRequired) 1 else 0)
        val actualNewLines = whiteSpaceBefore.text.count { it == '\n' }
        // for some cases (now - if this or previous property has custom accessors), blank line is allowed before it
        if (!hasCustomAccessors && actualNewLines != numRequiredNewLines ||
                hasCustomAccessors && actualNewLines > numRequiredNewLines) {
            BLANK_LINE_BETWEEN_PROPERTIES.warnAndFix(configRules, emitWarn, isFixMode, node.getIdentifierName()?.text ?: node.text, node.startOffset, node) {
                whiteSpaceBefore.leaveExactlyNumNewLines(numRequiredNewLines)
            }
        }
    }

    /**
     * Returns nested classes grouped by whether they are used inside [this] file.
     * [this] ASTNode should have elementType [CLASS_BODY]
     */
    private fun ASTNode.getUsedAndUnusedClasses() = getAllChildrenWithType(CLASS)
        .partition { classNode ->
            classNode.getIdentifierName()?.let { identifierNode ->
                parents()
                    .last()
                    .findAllDescendantsWithSpecificType(REFERENCE_EXPRESSION)
                    .any { ref ->
                        ref.parent({ it == classNode }) == null && ref.text.contains(identifierNode.text)
                    }
            } ?: false
        }

    /**
     * Checks whether all class elements in [this] node are correctly ordered and reorders them in fix mode.
     * [this] ASTNode should have elementType [CLASS_BODY]
     *
     * @param blocks list of class elements with leading whitespaces and comments
     */
    @Suppress("UnsafeCallOnNullableType")
    private fun ASTNode.checkAndReorderBlocks(blocks: List<List<ASTNode>>) {
        val classChildren = this.children().filter { it.elementType in childrenTypes }.toList()

        check(blocks.size == classChildren.size) {
            StringBuilder().apply {
                append("`classChildren` has a size of ${classChildren.size} while `blocks` has a size of ${blocks.size}$NEWLINE")

                append("`blocks`:$NEWLINE")
                blocks.forEachIndexed { index, block ->
                    append("\t$index: ${block.firstOrNull()?.text}$NEWLINE")
                }

                append("`classChildren`:$NEWLINE")
                classChildren.forEachIndexed { index, child ->
                    append("\t$index: ${child.text}$NEWLINE")
                }
            }
        }

        if (classChildren != blocks.map { it.first() }) {
            blocks.filterIndexed { index, pair -> classChildren[index] != pair.first() }
                .forEach { listOfChildren ->
                    val astNode = listOfChildren.first()
                    WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES.warnAndFix(configRules, emitWarn, isFixMode,
                        "${astNode.elementType}: ${astNode.findChildByType(IDENTIFIER)?.text ?: astNode.text}", astNode.startOffset, astNode) {
                        removeRange(findChildByType(LBRACE)!!.treeNext, findChildByType(RBRACE)!!)
                        blocks.reversed()
                            .forEach { bodyChild ->
                                bodyChild.forEach { this.addChild(it, this.children().take(2).last()) }
                            }
                        // Add newline before the closing `}`. All other newlines will be properly formatted by `NewlinesRule`.
                        this.addChild(PsiWhiteSpaceImpl("\n"), this.lastChildNode)
                    }
                }
        }
    }

    /**
     * Data class containing different groups of properties in file
     *
     * @property loggers loggers (for example, properties called `log` or `logger`)
     * @property constProperties `const val`s
     * @property properties all other properties
     * @property lateInitProperties `lateinit var`s
     */
    private data class AllProperties(val loggers: List<ASTNode>,
                                     val constProperties: List<ASTNode>,
                                     val properties: List<ASTNode>,
                                     val lateInitProperties: List<ASTNode>
    ) {
        companion object {
            /**
             * Create [AllProperties] wrapper from node with type [CLASS_BODY]
             *
             * @param node an ASTNode with type [CLASS_BODY]
             * @return an instance of [AllProperties]
             */
            @Suppress("UnsafeCallOnNullableType")
            fun fromClassBody(node: ASTNode): AllProperties {
                val allProperties = node.getAllChildrenWithType(PROPERTY)
                val constProperties = allProperties.filterByModifier(CONST_KEYWORD)
                val lateInitProperties = allProperties.filterByModifier(LATEINIT_KEYWORD)
                val loggers = allProperties.filterByModifier(PRIVATE_KEYWORD)
                    .filterNot { astNode ->
                        /*
                         * A `const` field named "logger" is unlikely to be a logger.
                         */
                        astNode in constProperties
                    }
                    .filterNot { astNode ->
                        /*
                         * A `lateinit` field named "logger" is unlikely to be a logger.
                         */
                        astNode in lateInitProperties
                    }
                    .filter { astNode ->
                        astNode.getIdentifierName()?.text?.matches(loggerPropertyRegex) ?: false
                    }
                val properties = allProperties.filter { it !in lateInitProperties && it !in loggers && it !in constProperties }
                return AllProperties(loggers, constProperties, properties, lateInitProperties)
            }
        }
    }

    /**
     * @property enumEntries if this class is a enum class, list of its entries. Otherwise an empty list.
     * @property allProperties an instance of [AllProperties]
     * @property objects objects
     * @property initBlocks `init` blocks
     * @property constructors constructors
     * @property methods functions
     * @property usedClasses nested classes that are used in the enclosing class
     * @property companion `companion object`s
     * @property unusedClasses nested classes that are *not* used in the enclosing class
     */
    private data class Blocks(val enumEntries: List<ASTNode>,
                              val allProperties: AllProperties,
                              val objects: List<ASTNode>,
                              val initBlocks: List<ASTNode>,
                              val constructors: List<ASTNode>,
                              val methods: List<ASTNode>,
                              val usedClasses: List<ASTNode>,
                              val companion: List<ASTNode>,
                              val unusedClasses: List<ASTNode>
    ) {
        init {
            require(companion.size in 0..1) { "There is more than one companion object in class" }
        }

        /**
         * @return all groups of structures in the class
         */
        fun allBlocks() = with(allProperties) {
            listOf(enumEntries, loggers, constProperties, properties, lateInitProperties, objects,
                initBlocks, constructors, methods, usedClasses, companion, unusedClasses)
        }

        /**
         * @return all blocks as a flattened list of [ASTNode]s
         */
        fun allBlockFlattened() = allBlocks().flatten()
    }

    companion object {
        const val NAME_ID = "class-like-structures"
        private val childrenTypes = listOf(PROPERTY, CLASS, CLASS_INITIALIZER, SECONDARY_CONSTRUCTOR, FUN, OBJECT_DECLARATION, ENUM_ENTRY)
    }
}

private fun Iterable<ASTNode>.filterByModifier(modifier: IElementType) = filter {
    it.findLeafWithSpecificType(modifier) != null
}
