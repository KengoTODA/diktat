@file:Suppress("FILE_UNORDERED_IMPORTS")// False positives, see #1494.

package org.cqfn.diktat.ruleset.chapter3.spaces

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.WRONG_INDENTATION
import org.cqfn.diktat.ruleset.junit.NaturalDisplayName
import org.cqfn.diktat.ruleset.rules.chapter3.files.IndentationRule
import org.cqfn.diktat.ruleset.utils.indentation.IndentationConfig.Companion.ALIGNED_PARAMETERS
import org.cqfn.diktat.ruleset.utils.indentation.IndentationConfig.Companion.EXTENDED_INDENT_AFTER_OPERATORS
import org.cqfn.diktat.ruleset.utils.indentation.IndentationConfig.Companion.EXTENDED_INDENT_BEFORE_DOT
import org.cqfn.diktat.ruleset.utils.indentation.IndentationConfig.Companion.EXTENDED_INDENT_FOR_EXPRESSION_BODIES
import org.cqfn.diktat.ruleset.utils.indentation.IndentationConfig.Companion.EXTENDED_INDENT_OF_PARAMETERS
import org.cqfn.diktat.ruleset.utils.indentation.IndentationConfig.Companion.NEWLINE_AT_END
import org.cqfn.diktat.test.framework.processing.FileComparisonResult
import org.cqfn.diktat.util.FixTestBase

import generated.WarningNames
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

import org.cqfn.diktat.ruleset.chapter3.spaces.IndentationConfigFactory as IndentationConfig

/**
 * Legacy indentation tests.
 *
 * Consider adding new tests to [IndentationRuleTest] instead.
 *
 * @see IndentationRuleTest
 */
@TestMethodOrder(NaturalDisplayName::class)
class IndentationRuleFixTest : FixTestBase("test/paragraph3/indentation",
    ::IndentationRule,
    listOf(
        RulesConfig(WRONG_INDENTATION.name, true,
            mapOf(
                NEWLINE_AT_END to "true",  // expected file should have two newlines at end in order to be read by BufferedReader correctly
                EXTENDED_INDENT_OF_PARAMETERS to "true",
                ALIGNED_PARAMETERS to "true",
                EXTENDED_INDENT_FOR_EXPRESSION_BODIES to "true",
                EXTENDED_INDENT_AFTER_OPERATORS to "true",
                EXTENDED_INDENT_BEFORE_DOT to "true",
            )
        )
    )
) {
    @Test
    @Tag(WarningNames.WRONG_INDENTATION)
    fun `parameters should be properly aligned`() {
        fixAndCompare("IndentationParametersExpected.kt", "IndentationParametersTest.kt")
    }

    @Test
    @Tag(WarningNames.WRONG_INDENTATION)
    fun `indentation rule - example 1`() {
        fixAndCompare("IndentationFull1Expected.kt", "IndentationFull1Test.kt")
    }

    @Test
    @Tag(WarningNames.WRONG_INDENTATION)
    fun `indentation rule - verbose example from ktlint`() {
        fixAndCompare("IndentFullExpected.kt", "IndentFullTest.kt")
    }

    @Test
    @Tag(WarningNames.WRONG_INDENTATION)
    fun `regression - incorrect fixing in constructor parameter list`() {
        fixAndCompare("ConstructorExpected.kt", "ConstructorTest.kt")
    }

    @Nested
    @TestMethodOrder(NaturalDisplayName::class)
    inner class `Multi-line string literals` {
        /**
         * Correctly-indented opening quotation mark, incorrectly-indented
         * closing quotation mark.
         */
        @Test
        @Tag(WarningNames.WRONG_INDENTATION)
        @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")  // False positives
        fun `case 1 - mis-aligned opening and closing quotes`(@TempDir tempDir: Path) {
            val actualCode = """
            |fun f() {
            |    g(
            |        ""${'"'}
            |            |val q = 1
            |            |
            |                    ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val expectedCode = """
            |fun f() {
            |    g(
            |        ""${'"'}
            |            |val q = 1
            |            |
            |        ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val lintResult = fixAndCompareContent(actualCode, expectedCode, tempDir)
            assertThat(lintResult.actualContent)
                .describedAs("lint result for ${actualCode.describe()}")
                .isEqualTo(lintResult.expectedContent)
        }

        /**
         * Both the opening and the closing quotation marks are incorrectly
         * indented (indentation level is less than needed).
         */
        @Test
        @Tag(WarningNames.WRONG_INDENTATION)
        @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")  // False positives
        fun `case 2`(@TempDir tempDir: Path) {
            val actualCode = """
            |fun f() {
            |    g(
            |    ""${'"'}
            |            |val q = 1
            |            |
            |    ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val expectedCode = """
            |fun f() {
            |    g(
            |        ""${'"'}
            |                |val q = 1
            |                |
            |        ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val lintResult = fixAndCompareContent(actualCode, expectedCode, tempDir)
            assertThat(lintResult.actualContent)
                .describedAs("lint result for ${actualCode.describe()}")
                .isEqualTo(lintResult.expectedContent)
        }

        /**
         * Both the opening and the closing quotation marks are incorrectly
         * indented (indentation level is greater than needed).
         */
        @Test
        @Tag(WarningNames.WRONG_INDENTATION)
        @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")  // False positives
        fun `case 3`(@TempDir tempDir: Path) {
            val actualCode = """
            |fun f() {
            |    g(
            |            ""${'"'}
            |                    |val q = 1
            |                    |
            |            ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val expectedCode = """
            |fun f() {
            |    g(
            |        ""${'"'}
            |                |val q = 1
            |                |
            |        ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val lintResult = fixAndCompareContent(actualCode, expectedCode, tempDir)
            assertThat(lintResult.actualContent)
                .describedAs("lint result for ${actualCode.describe()}")
                .isEqualTo(lintResult.expectedContent)
        }

        /**
         * Both the opening and the closing quotation marks are incorrectly
         * indented and misaligned.
         */
        @Test
        @Tag(WarningNames.WRONG_INDENTATION)
        @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")  // False positives
        fun `case 4 - mis-aligned opening and closing quotes`(@TempDir tempDir: Path) {
            val actualCode = """
            |fun f() {
            |    g(
            |            ""${'"'}
            |                    |val q = 1
            |                    |
            |                            ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val expectedCode = """
            |fun f() {
            |    g(
            |        ""${'"'}
            |                |val q = 1
            |                |
            |        ""${'"'}.trimMargin(),
            |        arg1 = "arg1"
            |    )
            |}
            """.trimMargin()

            val lintResult = fixAndCompareContent(actualCode, expectedCode, tempDir)
            assertThat(lintResult.actualContent)
                .describedAs("lint result for ${actualCode.describe()}")
                .isEqualTo(lintResult.expectedContent)
        }

        private fun fixAndCompareContent(@Language("kotlin") actualCode: String,
                                         @Language("kotlin") expectedCode: String,
                                         tempDir: Path
        ): FileComparisonResult {
            val config = IndentationConfig(NEWLINE_AT_END to false).withCustomParameters().asRulesConfigList()
            return fixAndCompareContent(actualCode, expectedCode, tempDir, config)
        }
    }
}
