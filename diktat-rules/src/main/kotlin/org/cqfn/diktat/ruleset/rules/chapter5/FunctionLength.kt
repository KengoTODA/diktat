package org.cqfn.diktat.ruleset.rules.chapter5

import org.cqfn.diktat.common.config.rules.RuleConfiguration
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.getRuleConfig
import org.cqfn.diktat.ruleset.constants.Warnings.TOO_LONG_FUNCTION
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.*

import com.pinterest.ktlint.core.ast.ElementType.FUN
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtFunction

/**
 * Rule 5.1.1 check function length
 */
class FunctionLength(configRules: List<RulesConfig>) : DiktatRule(
    NAME_ID,
    configRules,
    listOf(TOO_LONG_FUNCTION)
) {
    override fun logic(node: ASTNode) {
        val configuration = FunctionLengthConfiguration(
            configRules.getRuleConfig(TOO_LONG_FUNCTION)?.configuration ?: emptyMap()
        )

        if (node.elementType == FUN) {
            checkFun(node, configuration)
        }
    }

    private fun checkFun(node: ASTNode, configuration: FunctionLengthConfiguration) {
        val copyNode = if (configuration.isIncludeHeader) {
            node.clone() as ASTNode
        } else {
            ((node.psi as KtFunction)
                .bodyExpression
                ?.node
                ?.clone() ?: return) as ASTNode
        }
        val sizeFun = countCodeLines(copyNode)
        if (sizeFun > configuration.maxFunctionLength) {
            TOO_LONG_FUNCTION.warn(configRules, emitWarn, isFixMode,
                "max length is ${configuration.maxFunctionLength}, but you have $sizeFun",
                node.startOffset, node)
        }
    }

    /**
     * [RuleConfiguration] for function length
     */
    class FunctionLengthConfiguration(config: Map<String, String>) : RuleConfiguration(config) {
        /**
         * Maximum allowed function length
         */
        val maxFunctionLength = config["maxFunctionLength"]?.toLong() ?: MAX_FUNCTION_LENGTH

        /**
         * Whether function header (start of a declaration with parameter list and return type) is counted too
         */
        val isIncludeHeader = config["isIncludeHeader"]?.toBoolean() ?: true
    }

    companion object {
        private const val MAX_FUNCTION_LENGTH = 30L
        const val NAME_ID = "function-length"
    }
}
