package com.shangan.teacherprep.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.shangan.teacherprep.data.ContentSection
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object DocumentParser {
    fun readText(context: Context, uri: Uri, fileName: String): String {
        val resolver = context.contentResolver
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "docx" -> readDocx(resolver, uri)
            "pdf" -> readPdf(context, resolver, uri)
            else -> resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
    }

    fun splitMarkdown(markdown: String, preferredHeadings: List<String>): List<ContentSection> {
        val headingRegex = Regex("""(?m)^#{1,6}\s+(.+?)\s*$""")
        val matches = headingRegex.findAll(markdown).toList()
        if (matches.isNotEmpty()) {
            return matches.mapIndexed { index, match ->
                val start = match.range.last + 1
                val end = matches.getOrNull(index + 1)?.range?.first ?: markdown.length
                ContentSection(title = match.groupValues[1].trim(), markdown = markdown.substring(start, end).trim())
            }.filter { it.markdown.isNotBlank() }
        }

        // Plain Word documents often use headings without Markdown markers.
        val headingPattern = preferredHeadings
            .filter { it.isNotBlank() }
            .joinToString("|") { Regex.escape(it) }
        if (headingPattern.isNotBlank()) {
            val plainHeadingRegex = Regex(
                """(?m)^\s*(?:[\u4E00\u4E8C\u4E09\u56DB\u4E94\u516D\u4E03\u516B\u4E5D\u5341]+[\u3001.\uFF0E]\s*)?($headingPattern)\s*[\uFF1A:]?\s*$""",
            )
            val plainMatches = plainHeadingRegex.findAll(markdown).toList()
            if (plainMatches.isNotEmpty()) {
                return plainMatches.mapIndexed { index, match ->
                    val start = match.range.last + 1
                    val end = plainMatches.getOrNull(index + 1)?.range?.first ?: markdown.length
                    ContentSection(
                        title = match.groupValues[1],
                        markdown = markdown.substring(start, end).trim(),
                    )
                }.filter { it.markdown.isNotBlank() }
            }
        }
        return listOf(ContentSection(title = preferredHeadings.firstOrNull() ?: "正文", markdown = markdown.trim()))
    }

    private fun readDocx(resolver: ContentResolver, uri: Uri): String {
        val xml = resolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                generateSequence { zip.nextEntry }
                    .firstOrNull { it.name == "word/document.xml" }
                    ?.let { zip.readBytes() }
            }
        } ?: return ""
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.inputStream())
        val paragraphs = document.getElementsByTagName("w:p")
        return buildString {
            for (index in 0 until paragraphs.length) {
                val texts = paragraphs.item(index).childNodes
                val paragraph = buildString {
                    fun collect(node: org.w3c.dom.Node) {
                        if (node.nodeName == "w:t") append(node.textContent)
                        for (childIndex in 0 until node.childNodes.length) collect(node.childNodes.item(childIndex))
                    }
                    for (childIndex in 0 until texts.length) collect(texts.item(childIndex))
                }
                if (paragraph.isNotBlank()) appendLine(paragraph)
            }
        }.trim()
    }

    private fun readPdf(context: Context, resolver: ContentResolver, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val tempDirectory = File(context.cacheDir, "pdfbox").apply { mkdirs() }
        val memoryUsage = MemoryUsageSetting
            .setupMixed(8L * 1024L * 1024L)
            .setTempDir(tempDirectory)
        val text = resolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream, memoryUsage).use { document ->
                PDFTextStripper().getText(document)
            }
        }?.trim().orEmpty()
        check(text.isNotBlank()) { "PDF 中没有可提取文字，可能是扫描版文件" }
        return text
    }
}
