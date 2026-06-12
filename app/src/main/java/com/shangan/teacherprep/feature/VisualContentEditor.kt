package com.shangan.teacherprep.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatIndentDecrease
import androidx.compose.material.icons.rounded.FormatIndentIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shangan.teacherprep.ui.RoundedCard

data class VisualLine(
    val text: String = "",
    val indent: Int = 0,
    val bullet: Boolean = true,
)

data class VisualSection(
    val title: String = "",
    val lines: List<VisualLine> = listOf(VisualLine()),
)

object VisualEditorCodec {
    fun parse(markdown: String, defaultSectionTitle: String): List<VisualSection> {
        val normalized = markdown.replace("\r\n", "\n")
        val headings = Regex("""(?m)^#{1,6}\s+(.+?)\s*$""").findAll(normalized).toList()
        if (headings.isEmpty()) {
            return listOf(VisualSection(defaultSectionTitle, parseLines(normalized)))
        }
        return headings.mapIndexed { index, heading ->
            val start = heading.range.last + 1
            val end = headings.getOrNull(index + 1)?.range?.first ?: normalized.length
            VisualSection(
                title = heading.groupValues[1].trim(),
                lines = parseLines(normalized.substring(start, end)),
            )
        }
    }

    fun toMarkdown(sections: List<VisualSection>): String {
        return sections.joinToString("\n\n") { section ->
            buildString {
                appendLine("# ${section.title.ifBlank { "未命名章节" }}")
                section.lines.forEach { line ->
                    append("    ".repeat(line.indent.coerceIn(0, 5)))
                    if (line.bullet) append("- ")
                    appendLine(line.text)
                }
            }.trimEnd()
        }
    }

    private fun parseLines(markdown: String): List<VisualLine> {
        val lines = markdown.lines().filter { it.isNotBlank() }.map { raw ->
            val leading = raw.takeWhile { it == ' ' || it == '\t' }
            val spaces = leading.fold(0) { total, character -> total + if (character == '\t') 4 else 1 }
            val content = raw.trimStart()
            val bulletMatch = Regex("""^(?:[-*]|\d+\.)\s+(.+)$""").find(content)
            VisualLine(
                text = bulletMatch?.groupValues?.get(1) ?: content,
                indent = (spaces / 4).coerceIn(0, 5),
                bullet = bulletMatch != null,
            )
        }
        return lines.ifEmpty { listOf(VisualLine()) }
    }
}

@Composable
fun VisualContentEditor(
    sections: List<VisualSection>,
    onChange: (List<VisualSection>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        sections.forEachIndexed { sectionIndex, section ->
            RoundedCard(
                Modifier.fillMaxWidth(),
                containerColor = if (sectionIndex % 2 == 0) Color(0xFFF5F9FF) else Color(0xFFFFF7F3),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = section.title,
                        onValueChange = { title ->
                            onChange(sections.updated(sectionIndex, section.copy(title = title)))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("章节标题") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    IconButton(
                        onClick = {
                            if (sections.size > 1) onChange(sections.filterIndexed { index, _ -> index != sectionIndex })
                        },
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, "删除章节", tint = Color.Gray)
                    }
                }
                section.lines.forEachIndexed { lineIndex, line ->
                    Surface(
                        color = Color(0xFFF8F8FA),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = (line.indent * 12).dp),
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        updateLine(sections, sectionIndex, lineIndex, line.copy(bullet = !line.bullet), onChange)
                                    },
                                    modifier = Modifier.width(68.dp),
                                ) {
                                    Text(if (line.bullet) "要点" else "正文", fontWeight = FontWeight.Bold)
                                }
                                OutlinedTextField(
                                    value = line.text,
                                    onValueChange = { text ->
                                        updateLine(sections, sectionIndex, lineIndex, line.copy(text = text), onChange)
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(if (line.bullet) "填写要点" else "填写正文") },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("层级 ${line.indent + 1}", color = Color.Gray)
                                IconButton(
                                    onClick = {
                                        updateLine(sections, sectionIndex, lineIndex, line.copy(indent = (line.indent - 1).coerceAtLeast(0)), onChange)
                                    },
                                ) { Icon(Icons.Rounded.FormatIndentDecrease, "减少层级") }
                                IconButton(
                                    onClick = {
                                        updateLine(sections, sectionIndex, lineIndex, line.copy(indent = (line.indent + 1).coerceAtMost(5)), onChange)
                                    },
                                ) { Icon(Icons.Rounded.FormatIndentIncrease, "增加层级") }
                                IconButton(
                                    onClick = {
                                        val lines = section.lines.filterIndexed { index, _ -> index != lineIndex }
                                            .ifEmpty { listOf(VisualLine()) }
                                        onChange(sections.updated(sectionIndex, section.copy(lines = lines)))
                                    },
                                ) { Icon(Icons.Rounded.DeleteOutline, "删除条目", tint = Color.Gray) }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        onChange(sections.updated(sectionIndex, section.copy(lines = section.lines + VisualLine())))
                    },
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Text(" 添加条目")
                }
            }
        }
        OutlinedButton(
            onClick = { onChange(sections + VisualSection(title = "新章节")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Add, null)
            Text(" 添加章节")
        }
    }
}

private fun <T> List<T>.updated(index: Int, value: T): List<T> {
    return mapIndexed { itemIndex, item -> if (itemIndex == index) value else item }
}

private fun updateLine(
    sections: List<VisualSection>,
    sectionIndex: Int,
    lineIndex: Int,
    line: VisualLine,
    onChange: (List<VisualSection>) -> Unit,
) {
    val section = sections[sectionIndex]
    onChange(
        sections.updated(
            sectionIndex,
            section.copy(lines = section.lines.updated(lineIndex, line)),
        ),
    )
}
