package com.shangan.teacherprep.data

/**
 * Converts the bundled junior-high Chinese notes into maintainable trial lessons.
 * Parsing is kept independent from Android APIs so the format can be unit tested.
 */
object BundledTrialCatalog {
    const val ASSET_DIRECTORY = "junior_chinese_trials"
    private val scope = LibraryScope("初中", "语文", "人教版")

    private val genres = mapOf(
        "寓言.md" to "寓言",
        "小说.md" to "小说",
        "文言.md" to "文言文",
        "新闻演讲.md" to "新闻演讲",
        "说明文.md" to "说明文",
        "议论文.md" to "议论文",
        "现代诗.md" to "现代诗",
        "现代叙事散文.md" to "叙事散文",
        "现代写物散文（托物言志）.md" to "托物言志散文",
        "现代写景散文.md" to "写景散文",
        "现代写人散文.md" to "写人散文",
    )

    fun parse(fileName: String, markdown: String): List<TrialLesson> {
        val genre = genres[fileName] ?: return emptyList()
        val document = markdown.replace("\r\n", "\n")
        val headings = Regex("""(?m)^#\s+(.+?)\s*$""").findAll(document).toList()

        return headings.mapIndexedNotNull { index, heading ->
            val rawHeading = heading.groupValues[1].trim()
            if (rawHeading.contains("注意") || rawHeading.contains("考情")) return@mapIndexedNotNull null

            val start = heading.range.last + 1
            val end = headings.getOrNull(index + 1)?.range?.first ?: document.length
            parseLesson(fileName, genre, index, rawHeading, document.substring(start, end))
        }
    }

    private fun parseLesson(
        fileName: String,
        genre: String,
        index: Int,
        rawHeading: String,
        content: String,
    ): TrialLesson {
        val importance = when {
            rawHeading.contains("⭐") -> 5
            rawHeading.contains("✊") -> 4
            rawHeading.contains("📚") -> 3
            else -> 3
        }
        val cleanedHeading = rawHeading
            .replace("⭐", "")
            .replace("✊️", "")
            .replace("✊", "")
            .replace("📚", "")
            .replace("==", "")
            .replace(Regex("""\[教学设计].*$"""), "")
            .trim()
        val textbookCode = Regex("""（([^）]+)）""").find(cleanedHeading)?.groupValues?.get(1).orEmpty()
        val withoutTextbook = cleanedHeading.replace(Regex("""（[^）]+）"""), "").trim()
        val parts = withoutTextbook.split("——", limit = 3)
        val rawTitle = parts.firstOrNull().orEmpty().trim()
        val author = parts.getOrNull(1).orEmpty().trim()
        val title = if (rawTitle.contains('《')) rawTitle else "《$rawTitle》"
        val textbook = textbookFrom(textbookCode)
        val unit = unitFrom(textbookCode)
        val lessonOrder = lessonOrderFrom(textbookCode)
        val sections = splitSecondLevelSections(content)
        val basicInfo = sections.firstOrNull { it.first.contains("基本信息") }?.second.orEmpty()
        val teachingSteps = sections.firstOrNull { it.first.contains("教学步骤") }?.second.orEmpty()
        val courseInfo = buildString {
            appendLine("# 课程信息")
            appendLine("- 教材：$textbook")
            appendLine("- 单元：$unit")
            if (lessonOrder > 0) appendLine("- 课时：第${lessonOrder}课")
            appendLine("- 题材：$genre")
            if (author.isNotBlank()) appendLine("- 作者：$author")
            appendLine("- 资料来源：$fileName")
            if (basicInfo.isNotBlank()) {
                appendLine()
                appendLine("## 基本信息")
                append(basicInfo)
            }
        }.trim()

        return TrialLesson(
            id = "builtin-junior-chinese-${fileName.hashCode()}-$index",
            scopeKey = scope.key,
            title = title,
            author = author,
            textbook = textbook,
            unit = unit,
            lessonOrder = lessonOrder,
            genre = genre,
            courseInfoMarkdown = courseInfo,
            bodySections = splitTeachingSteps(teachingSteps),
            importance = importance,
        )
    }

    private fun splitSecondLevelSections(content: String): List<Pair<String, String>> {
        val headings = Regex("""(?m)^##\s+(.+?)\s*$""").findAll(content).toList()
        return headings.mapIndexed { index, heading ->
            val start = heading.range.last + 1
            val end = headings.getOrNull(index + 1)?.range?.first ?: content.length
            heading.groupValues[1].trim() to content.substring(start, end).trim()
        }
    }

    private fun splitTeachingSteps(markdown: String): List<ContentSection> {
        if (markdown.isBlank()) {
            return listOf(ContentSection(title = "教学步骤", markdown = "原始资料暂未填写教学步骤，可点击修改继续完善。"))
        }

        val lines = markdown.lines()
        val starts = lines.indices.filter { lines[it].startsWith("- ") }
        if (starts.isEmpty()) return listOf(ContentSection(title = "教学步骤", markdown = markdown))

        return starts.mapIndexed { index, start ->
            val end = starts.getOrNull(index + 1) ?: lines.size
            val title = lines[start].removePrefix("- ").trim().ifBlank { "教学步骤 ${index + 1}" }
            val body = lines.subList(start + 1, end)
                .joinToString("\n") { it.removePrefix("\t").removePrefix("    ") }
                .trim()
                .ifBlank { title }
            ContentSection(
                id = "step-$index-${title.hashCode()}",
                title = title,
                markdown = body,
            )
        }
    }

    private fun textbookFrom(code: String): String {
        val grade = when {
            code.startsWith("七") -> "七年级"
            code.startsWith("八") -> "八年级"
            code.startsWith("九") -> "九年级"
            else -> "初中通用"
        }
        val volume = when {
            code.contains("上") -> "上册"
            code.contains("下") -> "下册"
            else -> ""
        }
        return grade + volume
    }

    private fun unitFrom(code: String): String {
        val numeral = Regex("""[上下]([一二三四五六七八九十]+)""")
            .find(code)
            ?.groupValues
            ?.get(1)
            .orEmpty()
        return if (numeral.isBlank()) "其他" else "第${numeral}单元"
    }

    private fun lessonOrderFrom(code: String): Int {
        return Regex("""(\d+)$""").find(code)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}
