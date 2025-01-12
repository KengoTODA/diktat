package org.cqfn.diktat.ruleset.rules.chapter5

import org.cqfn.diktat.common.config.rules.RuleConfiguration
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.getRuleConfig
import org.cqfn.diktat.ruleset.constants.Warnings.TOO_MANY_LINES_IN_LAMBDA
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.*

import com.pinterest.ktlint.core.ast.ElementType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Rule 5.2.5 check lambda length without parameters
 */
class LambdaLengthRule(configRules: List<RulesConfig>) : DiktatRule(
    NAME_ID,
    configRules,
    listOf(TOO_MANY_LINES_IN_LAMBDA)
) {
    private val configuration by lazy {
        LambdaLengthConfiguration(
            this.configRules.getRuleConfig(TOO_MANY_LINES_IN_LAMBDA)?.configuration ?: emptyMap()
        )
    }

    override fun logic(node: ASTNode) {
        if (node.elementType == ElementType.LAMBDA_EXPRESSION) {
            checkLambda(node, configuration)
        }
    }

    private fun checkLambda(node: ASTNode, configuration: LambdaLengthConfiguration) {
        val sizeLambda = countCodeLines(node)
        if (sizeLambda > configuration.maxLambdaLength && doesLambdaContainIt(node)) {
            TOO_MANY_LINES_IN_LAMBDA.warn(configRules, emitWarn, isFixMode,
                "max length lambda without arguments is ${configuration.maxLambdaLength}, but you have $sizeLambda",
                node.startOffset, node)
        }
    }

    /**
     * [RuleConfiguration] for lambda length
     */
    class LambdaLengthConfiguration(config: Map<String, String>) : RuleConfiguration(config) {
        /**
         * Maximum allowed lambda length
         */
        val maxLambdaLength = config["maxLambdaLength"]?.toLong() ?: MAX_LINES_IN_LAMBDA
    }

    companion object {
        private const val MAX_LINES_IN_LAMBDA = 10L
        const val NAME_ID = "lambda-length"
    }
}
