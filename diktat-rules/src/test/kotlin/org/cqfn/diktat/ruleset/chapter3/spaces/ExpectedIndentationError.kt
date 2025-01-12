package org.cqfn.diktat.ruleset.chapter3.spaces

import org.cqfn.diktat.common.config.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.ruleset.constants.Warnings.WRONG_INDENTATION
import org.cqfn.diktat.ruleset.junit.ExpectedLintError
import org.cqfn.diktat.ruleset.rules.chapter3.files.IndentationRule.Companion.NAME_ID
import com.pinterest.ktlint.core.LintError

/**
 * The expected indentation error (extracted from annotated code fragments).
 *
 * @property line the line number (1-based).
 * @property column the column number (1-based).
 */
class ExpectedIndentationError(override val line: Int,
                               override val column: Int = 1,
                               private val message: String
) : ExpectedLintError {
    /**
     * @param line the line number (1-based).
     * @param column the column number (1-based).
     * @param expectedIndent the expected indentation level (in space characters).
     * @param actualIndent the actual indentation level (in space characters).
     */
    constructor(line: Int,
                column: Int = 1,
                expectedIndent: Int,
                actualIndent: Int
    ) : this(
        line,
        column,
        warnText(expectedIndent)(actualIndent)
    )

    override fun asLintError(): LintError =
        LintError(
            line,
            column,
            "$DIKTAT_RULE_SET_ID:$NAME_ID",
            message,
            true)

    private companion object {
        private val warnText: (Int) -> (Int) -> String = { expectedIndent ->
            { actualIndent ->
                "${WRONG_INDENTATION.warnText()} expected $expectedIndent but was $actualIndent"
            }
        }
    }
}
