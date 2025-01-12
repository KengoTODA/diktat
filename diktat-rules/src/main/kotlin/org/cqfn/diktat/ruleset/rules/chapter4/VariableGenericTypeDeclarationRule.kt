package org.cqfn.diktat.ruleset.rules.chapter4

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.GENERIC_VARIABLE_WRONG_DECLARATION
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.getAllChildrenWithType

import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

/**
 * This Rule checks if declaration of a generic variable is appropriate.
 * Not recommended: val myVariable: Map<Int, String> = emptyMap<Int, String>() or val myVariable = emptyMap<Int, String>()
 * Recommended: val myVariable: Map<Int, String> = emptyMap()
 */
// FIXME: we now don't have access to return types, so we can perform this check only if explicit type is present, but should be able also if it's not.
class VariableGenericTypeDeclarationRule(configRules: List<RulesConfig>) : DiktatRule(
    NAME_ID,
    configRules,
    listOf(GENERIC_VARIABLE_WRONG_DECLARATION)
) {
    override fun logic(node: ASTNode) {
        when (node.elementType) {
            PROPERTY, VALUE_PARAMETER -> handleProperty(node)
            else -> {
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType", "AVOID_NULL_CHECKS")
    private fun handleProperty(node: ASTNode) {
        val callExpr = node.findChildByType(CALL_EXPRESSION)
            ?: node
                .findChildByType(DOT_QUALIFIED_EXPRESSION)
                ?.getAllChildrenWithType(CALL_EXPRESSION)
                ?.lastOrNull()

        val rightSide = (callExpr?.psi as? KtCallExpression)?.typeArgumentList?.arguments
        val leftSide = if (node.elementType == PROPERTY) {
            (node.psi as? KtProperty)
                ?.typeReference
                ?.typeElement
                ?.typeArgumentsAsTypes
        } else {
            (node.psi as? KtParameter)
                ?.typeReference
                ?.typeElement
                ?.typeArgumentsAsTypes
        }

        // Allow cases with wild card types; `*` interprets as `null` in list of types
        if (leftSide?.any { it == null } == true) {
            return
        }

        if (rightSide != null && leftSide != null &&
                rightSide.size == leftSide.size &&
                rightSide.zip(leftSide).all { (first, second) -> first.text == second.text }) {
            GENERIC_VARIABLE_WRONG_DECLARATION.warnAndFix(configRules, emitWarn, isFixMode,
                "type arguments are unnecessary in ${callExpr.text}", node.startOffset, node) {
                callExpr.removeChild(callExpr.findChildByType(TYPE_ARGUMENT_LIST)!!)
            }
        }

        if (leftSide == null && rightSide != null) {
            GENERIC_VARIABLE_WRONG_DECLARATION.warn(configRules, emitWarn, isFixMode, node.text, node.startOffset, node)
        }
    }

    companion object {
        const val NAME_ID = "variable-generic-type"
    }
}
