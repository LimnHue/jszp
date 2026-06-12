package com.shangan.teacherprep.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentParserTest {
    @Test
    fun `splits markdown headings into editable sections`() {
        val result = DocumentParser.splitMarkdown(
            "# 导入新课\n情境导入\n# 整体感知\n快速默读课文",
            listOf("导入新课", "整体感知"),
        )

        assertEquals(listOf("导入新课", "整体感知"), result.map { it.title })
        assertEquals("情境导入", result.first().markdown)
    }

    @Test
    fun `recognizes plain document headings`() {
        val result = DocumentParser.splitMarkdown(
            "一、导入新课\n情境导入\n二、整体感知\n快速默读课文",
            listOf("导入新课", "整体感知"),
        )

        assertEquals(2, result.size)
        assertEquals("整体感知", result.last().title)
    }
}
