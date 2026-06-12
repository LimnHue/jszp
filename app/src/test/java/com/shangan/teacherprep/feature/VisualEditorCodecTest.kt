package com.shangan.teacherprep.feature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualEditorCodecTest {
    @Test
    fun `preserves nested bullet hierarchy across editor modes`() {
        val markdown = """
            # 任务三
            - 为什么文中写到这段回忆
                - 抗战的艰苦
                - 鱼水情
                - 对光明和美好未来的希冀
        """.trimIndent()

        val sections = VisualEditorCodec.parse(markdown, "正文")

        assertEquals(listOf(0, 1, 1, 1), sections.single().lines.map { it.indent })
        val rebuilt = VisualEditorCodec.toMarkdown(sections)
        assertTrue(rebuilt.contains("    - 抗战的艰苦"))
        assertTrue(rebuilt.contains("    - 鱼水情"))
    }

    @Test
    fun `plain text becomes an editable visual section`() {
        val sections = VisualEditorCodec.parse("这是一段正文", "模板正文")

        assertEquals("模板正文", sections.single().title)
        assertEquals(false, sections.single().lines.single().bullet)
    }
}
