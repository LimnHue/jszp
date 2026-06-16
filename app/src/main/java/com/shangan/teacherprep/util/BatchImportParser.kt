package com.shangan.teacherprep.util

data class BatchDocumentItem(
    val title: String,
    val markdown: String,
)

object BatchImportParser {
    private val numberedLineRegex = Regex("""^\s*(\d{1,3})[.．、]\s*(.*?)\s*$""")
    private val answerRegex = Regex("""^\s*(?:【?答】?|参考答案|答案)\s*[:：]?\s*(.*)$""")
    private val metadataRegex = Regex("""^\s*(?:\d+|目录|微博[:：].*|抖音[:：].*|微信公众号[:：].*|.*\d+道总题库.*)\s*$""")
    private val tocLeaderRegex = Regex("""\.{3,}|…{2,}|\u2026{2,}""")

    fun parseStructured(text: String): List<BatchDocumentItem> {
        return parseNumberedAnswerItems(text)
    }

    fun parseTrial(text: String): List<BatchDocumentItem> {
        val markdownHeadingItems = parseMarkdownHeadingItems(text)
        if (markdownHeadingItems.size > 1) return markdownHeadingItems
        return parseNumberedAnswerItems(text)
    }

    private fun parseMarkdownHeadingItems(text: String): List<BatchDocumentItem> {
        val headingRegex = Regex("""(?m)^#{1,2}\s+(.+?)\s*$""")
        val matches = headingRegex.findAll(text).toList()
        if (matches.size < 2) return emptyList()
        return matches.mapIndexedNotNull { index, match ->
            val title = cleanTitle(match.groupValues[1])
            val start = match.range.last + 1
            val end = matches.getOrNull(index + 1)?.range?.first ?: text.length
            val body = text.substring(start, end).trim()
            if (title.isBlank() || body.isBlank()) null else BatchDocumentItem(title, body)
        }
    }

    private fun parseNumberedAnswerItems(text: String): List<BatchDocumentItem> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val markers = lines.mapIndexedNotNull { index, line ->
            val match = numberedLineRegex.matchEntire(line) ?: return@mapIndexedNotNull null
            val number = match.groupValues[1].toIntOrNull() ?: return@mapIndexedNotNull null
            val rest = match.groupValues[2].trim()
            if (number <= 0 || rest.contains(tocLeaderRegex)) return@mapIndexedNotNull null
            Marker(index, number, rest)
        }
        if (markers.size < 2) return emptyList()

        val entries = markers.mapIndexedNotNull { markerIndex, marker ->
            val nextMarker = markers.getOrNull(markerIndex + 1)
            val answerIndex = findAnswerIndex(lines, marker.index, nextMarker?.index ?: lines.size)
                ?: return@mapIndexedNotNull null
            val titleStart = findTitleStart(lines, marker)
            val title = buildTitle(lines, titleStart, marker, answerIndex)
            val nextTitleStart = nextMarker?.let { findTitleStart(lines, it) } ?: lines.size
            val answer = buildAnswer(lines, answerIndex, nextTitleStart)
            if (title.isBlank() || answer.isBlank()) null else BatchDocumentItem(title, answer)
        }
        return entries.distinctBy { normalizeTitle(it.title) }
    }

    private fun findAnswerIndex(lines: List<String>, markerIndex: Int, nextMarkerIndex: Int): Int? {
        return ((markerIndex + 1) until nextMarkerIndex)
            .firstOrNull { answerRegex.containsMatchIn(lines[it]) }
    }

    private fun findTitleStart(lines: List<String>, marker: Marker): Int {
        if (marker.rest.isNotBlank()) return marker.index
        val previousIndex = marker.index - 1
        if (previousIndex < 0) return marker.index
        val previous = lines[previousIndex]
        return if (isUsableTitleLine(previous)) previousIndex else marker.index
    }

    private fun buildTitle(
        lines: List<String>,
        titleStart: Int,
        marker: Marker,
        answerIndex: Int,
    ): String {
        val titleLines = buildList {
            if (titleStart < marker.index) add(lines[titleStart])
            if (marker.rest.isNotBlank()) add(marker.rest)
            ((marker.index + 1) until answerIndex).forEach { index ->
                val line = lines[index]
                if (!answerRegex.containsMatchIn(line) && !isMetadataLine(line)) add(line)
            }
        }
        return cleanTitle(titleLines.joinToString(""))
    }

    private fun buildAnswer(lines: List<String>, answerIndex: Int, endExclusive: Int): String {
        return ((answerIndex) until endExclusive)
            .mapNotNull { index ->
                val line = lines[index]
                if (isMetadataLine(line)) return@mapNotNull null
                answerRegex.matchEntire(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                    ?: line.takeIf { !answerRegex.matches(it) }
            }
            .joinToString("\n")
            .trim()
    }

    private fun isUsableTitleLine(line: String): Boolean {
        if (isMetadataLine(line)) return false
        if (answerRegex.containsMatchIn(line)) return false
        if (numberedLineRegex.matches(line)) return false
        if (line.startsWith("第") || line.startsWith("答")) return false
        return line.length <= 60
    }

    private fun isMetadataLine(line: String): Boolean {
        return metadataRegex.matches(line) || line.contains(tocLeaderRegex)
    }

    private fun cleanTitle(raw: String): String {
        return raw
            .replace("【模拟题】", "")
            .replace("[模拟题]", "")
            .replace(Regex("""\s+"""), "")
            .trim(' ', '。', '.', '．')
            .take(80)
    }

    private fun normalizeTitle(title: String): String {
        return title.replace(Regex("""\s+|[，。,.．？?！!]"""), "")
    }

    private data class Marker(
        val index: Int,
        val number: Int,
        val rest: String,
    )
}
