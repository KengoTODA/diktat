package org.cqfn.diktat.ruleset.chapter3

import org.cqfn.diktat.ruleset.rules.chapter3.EnumsSeparated
import org.cqfn.diktat.util.FixTestBase

import generated.WarningNames
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class EnumsSeparatedFixTest : FixTestBase("test/paragraph3/enum_separated", ::EnumsSeparated) {
    @Test
    @Tag(WarningNames.ENUMS_SEPARATED)
    fun `test enums split`() {
        fixAndCompare("EnumSeparatedExpected.kt", "EnumSeparatedTest.kt")
    }

    @Test
    @Tag(WarningNames.ENUMS_SEPARATED)
    fun `last element with comment`() {
        fixAndCompare("LastElementCommentExpected.kt", "LastElementCommentTest.kt")
    }
}
