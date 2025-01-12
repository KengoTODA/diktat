package org.cqfn.diktat.ruleset.chapter3.spaces.junit

import org.cqfn.diktat.ruleset.chapter3.spaces.asRulesConfigList
import org.cqfn.diktat.ruleset.chapter3.spaces.describe
import org.cqfn.diktat.ruleset.chapter3.spaces.withCustomParameters
import org.cqfn.diktat.ruleset.junit.CloseablePath
import org.cqfn.diktat.ruleset.rules.chapter3.files.IndentationRule
import org.cqfn.diktat.util.FixTestBase
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

/**
 * The `Extension` implementation for indentation test templates (fix mode).
 *
 * @property customConfig non-default configuration for the indentation rule.
 * @property actualCode the original file content (may well get modified as
 *   fixes are applied).
 */
@Suppress(
    "TOO_MANY_BLANK_LINES",  // Readability
    "WRONG_INDENTATION")  // False positives, see #1404.
class IndentationTestFixExtension(
    override val customConfig: Map<String, Any>,
    @Language("kotlin") override val actualCode: String,
    @Language("kotlin") private val expectedCode: String
) : FixTestBase("nonexistent", ::IndentationRule),
    IndentationTestExtension,
    BeforeEachCallback {

    private lateinit var tempDir: Path

    override fun beforeEach(context: ExtensionContext) {
        tempDir = context.getStore(namespace).getOrComputeIfAbsent(KEY, {
            CloseablePath(createTempDirectory(prefix = TEMP_DIR_PREFIX))
        }, CloseablePath::class.java).directory
    }

    override fun beforeTestExecution(context: ExtensionContext) {
        val lintResult = fixAndCompareContent(
            actualCode,
            expectedCode,
            tempDir,
            defaultConfig.withCustomParameters(customConfig).asRulesConfigList())

        if (!lintResult.isSuccessful) {
            assertThat(lintResult.actualContent)
                .describedAs("lint result for ${actualCode.describe()}")
                .isEqualTo(lintResult.expectedContent)
        }
    }

    private companion object {
        private const val KEY = "temp.dir"
        private const val TEMP_DIR_PREFIX = "junit"
        private val namespace = Namespace.create(IndentationTestFixExtension::class)
    }
}
