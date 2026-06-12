package com.shangan.teacherprep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledTrialCatalogTest {
    @Test
    fun `parses lesson metadata sections and importance`() {
        val lessons = BundledTrialCatalog.parse(
            "现代写景散文.md",
            """
                # ⭐ 春——朱自清（七上一1）
                ## 1⃣板书设计
                ## 2⃣基本信息
                - 目标
                  - 品味语言
                ## 3⃣教学步骤
                - 步骤一——感知
                  - 理清文章结构
                - 步骤二——精读
                  - 品味春景
            """.trimIndent(),
        )

        assertEquals(1, lessons.size)
        assertEquals("初中::语文::人教版", lessons.single().scopeKey)
        assertEquals("《春》", lessons.single().title)
        assertEquals("朱自清", lessons.single().author)
        assertEquals("七年级上册", lessons.single().textbook)
        assertEquals("第一单元", lessons.single().unit)
        assertEquals(1, lessons.single().lessonOrder)
        assertEquals("写景散文", lessons.single().genre)
        assertEquals(5, lessons.single().importance)
        assertEquals(listOf("步骤一——感知", "步骤二——精读"), lessons.single().bodySections.map { it.title })
        assertTrue(lessons.single().courseInfoMarkdown.contains("资料来源：现代写景散文.md"))
    }

    @Test
    fun `maps study markers to star levels`() {
        val markdown = """
            # ✊️ 重点课程（八下）
            ## 3⃣教学步骤
            - 初读
            # 📚 常规课程——作者（九上）
            ## 3⃣教学步骤
            - 精读
        """.trimIndent()

        val lessons = BundledTrialCatalog.parse("文言.md", markdown)

        assertEquals(listOf(4, 3), lessons.map { it.importance })
        assertEquals(listOf("八年级下册", "九年级上册"), lessons.map { it.textbook })
        assertEquals(listOf("其他", "其他"), lessons.map { it.unit })
    }
}
