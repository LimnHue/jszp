package com.shangan.teacherprep.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchImportParserTest {
    @Test
    fun `parses structured pdf style numbered questions`() {
        val text = """
            目录
            1. 如果有同学屡次不交作业，你怎么办？...........................................................................3
            如果有同学屡次不交作业，你怎么办？
            1.
            【答】遇到这种情况我会冷静处理。
            第一，我会询问该同学不交作业的原因。
            一个女生总是以生理期为借口，不来上课，你怎么办？
            2.
            【答】这件事会引起我的重视。
            第一，我会联系学生家长调查具体情况。
            上课有学生在传纸条，旁边有学生看到了，打断了你上课
            3.
            向你报告，你怎么办？
            【答】我会及时妥善处理。
            第一，我会用眼神制止。
        """.trimIndent()

        val result = BatchImportParser.parseStructured(text)

        assertEquals(3, result.size)
        assertEquals("如果有同学屡次不交作业，你怎么办？", result[0].title)
        assertEquals("上课有学生在传纸条，旁边有学生看到了，打断了你上课向你报告，你怎么办？", result[2].title)
        assertTrue(result[0].markdown.contains("遇到这种情况"))
    }

    @Test
    fun `ignores single item imports`() {
        val result = BatchImportParser.parseStructured(
            """
                如果有同学屡次不交作业，你怎么办？
                1.
                【答】遇到这种情况我会冷静处理。
            """.trimIndent(),
        )

        assertTrue(result.isEmpty())
    }
}
