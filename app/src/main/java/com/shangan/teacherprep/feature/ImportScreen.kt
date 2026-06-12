package com.shangan.teacherprep.feature

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.ImportModule
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.ContentSection
import com.shangan.teacherprep.data.ScopeConfig
import com.shangan.teacherprep.ui.FilterChips
import com.shangan.teacherprep.ui.GradientActionButton
import com.shangan.teacherprep.ui.ImportanceStars
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScreenHeader
import com.shangan.teacherprep.util.DocumentParser

private enum class EditorMode { VISUAL, MARKDOWN }

@Composable
fun ImportScreen(
    module: ImportModule,
    editingId: String?,
    data: AppData,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAddTrial: (String, String, String, Int, String, String, List<ContentSection>, String?, Int) -> Unit,
    onAddStructured: (String, String, List<ContentSection>, Int) -> Unit,
    onAddTemplate: (String, String, String) -> Unit,
    onUpdateTrial: (String, String, String, String, Int, String, String, List<ContentSection>, String?, Int) -> Unit,
    onUpdateStructured: (String, String, String, List<ContentSection>, Int) -> Unit,
    onUpdateTemplate: (String, String, String, String) -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val config = data.scopeConfigs[data.preferences.selectedScope.key]
        ?: com.shangan.teacherprep.data.ScopeDefaults.create(data.preferences.selectedScope)
    val existingTrial = data.trials.firstOrNull { it.id == editingId }
    val existingStructured = data.structuredQuestions.firstOrNull { it.id == editingId }
    val existingTemplate = data.templates.firstOrNull { it.id == editingId }
    val editing = editingId != null
    val initialMarkdown = remember(editingId) {
        when {
            existingTrial != null -> buildTrialMarkdown(existingTrial)
            existingStructured != null -> buildSectionMarkdown(existingStructured.answerSections)
            existingTemplate != null -> existingTemplate.contentMarkdown
            else -> ""
        }
    }
    var title by remember(editingId) {
        mutableStateOf(existingTrial?.title ?: existingStructured?.question ?: existingTemplate?.name.orEmpty())
    }
    var markdown by remember(editingId) { mutableStateOf(initialMarkdown) }
    var editorMode by remember(editingId) { mutableStateOf(EditorMode.VISUAL) }
    var visualSections by remember(editingId) {
        mutableStateOf(VisualEditorCodec.parse(initialMarkdown, defaultSectionTitle(module, config)))
    }
    var fileName by remember { mutableStateOf<String?>(null) }
    var textbook by remember(editingId) {
        mutableStateOf(existingTrial?.textbook ?: config.textbooks.firstOrNull().orEmpty())
    }
    var unit by remember(editingId) {
        mutableStateOf(existingTrial?.unit?.ifBlank { null } ?: config.units.firstOrNull().orEmpty())
    }
    var lessonOrder by remember(editingId) {
        mutableStateOf(existingTrial?.lessonOrder?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var category by remember(editingId) {
        mutableStateOf(
            when (module) {
                ImportModule.TRIAL -> existingTrial?.genre ?: config.genres.firstOrNull().orEmpty()
                ImportModule.STRUCTURED -> existingStructured?.category ?: config.structuredTypes.firstOrNull().orEmpty()
                else -> existingTemplate?.category ?: config.templateTypes.firstOrNull().orEmpty()
            },
        )
    }
    var boardUri by remember(editingId) { mutableStateOf(existingTrial?.boardImageUri) }
    var importance by remember(editingId) {
        mutableStateOf(existingTrial?.importance ?: existingStructured?.importance ?: 3)
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else "导入文档"
            } ?: "导入文档"
            fileName = name
            markdown = runCatching { DocumentParser.readText(context.contentResolver, it, name) }.getOrElse { "读取失败：${it.message}" }
            visualSections = VisualEditorCodec.parse(markdown, defaultSectionTitle(module, config))
            if (title.isBlank()) title = name.substringBeforeLast('.')
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            boardUri = it.toString()
        }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = .025f))) {
        ScreenHeader(if (editing) "修改内容" else "导入内容", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (editing) "修改后将直接更新原内容" else "把你的资料整理进专属题库",
                color = Color.Gray,
                fontSize = 17.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ImportMethodCard("导入文档", Icons.Rounded.Description, fileName != null, Modifier.weight(1f)) {
                    documentPicker.launch(arrayOf("text/markdown", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                }
                ImportMethodCard("手动新建", Icons.Rounded.Edit, fileName == null, Modifier.weight(1f)) {
                    fileName = null
                    markdown = ""
                    visualSections = listOf(VisualSection(title = defaultSectionTitle(module, config)))
                }
            }
            fileName?.let {
                RoundedCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text("  $it", fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedTextField(
                title,
                { title = it },
                Modifier.fillMaxWidth(),
                label = { Text(titleLabel(module)) },
                shape = RoundedCornerShape(16.dp),
            )
            if (module == ImportModule.TRIAL) {
                Text("教材", fontWeight = FontWeight.Bold)
                FilterChips(config.textbooks, textbook, { textbook = it }, includeAll = false)
                Text("单元", fontWeight = FontWeight.Bold)
                FilterChips(config.units, unit, { unit = it }, includeAll = false)
                OutlinedTextField(
                    value = lessonOrder,
                    onValueChange = { value -> lessonOrder = value.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("课时序号（例如：1）") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Text("题材", fontWeight = FontWeight.Bold)
                FilterChips(config.genres, category, { category = it }, includeAll = false)
            } else {
                Text(if (module == ImportModule.STRUCTURED) "问题种类" else "模板种类", fontWeight = FontWeight.Bold)
                FilterChips(
                    if (module == ImportModule.STRUCTURED) config.structuredTypes else config.templateTypes,
                    category,
                    { category = it },
                    includeAll = false,
                )
            }
            if (module == ImportModule.TRIAL || module == ImportModule.STRUCTURED) {
                Text("重要程度", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImportanceStars(
                        value = importance,
                        onValueChange = { importance = it },
                        iconSize = 34,
                    )
                    Text("  $importance 星", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
            Text("正文编辑", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorModeCard(
                    title = "分段编辑",
                    selected = editorMode == EditorMode.VISUAL,
                    modifier = Modifier.weight(1f),
                ) {
                    if (editorMode != EditorMode.VISUAL) {
                        visualSections = VisualEditorCodec.parse(markdown, defaultSectionTitle(module, config))
                        editorMode = EditorMode.VISUAL
                    }
                }
                EditorModeCard(
                    title = "文本编辑",
                    selected = editorMode == EditorMode.MARKDOWN,
                    modifier = Modifier.weight(1f),
                ) {
                    if (editorMode != EditorMode.MARKDOWN) {
                        markdown = VisualEditorCodec.toMarkdown(visualSections)
                        editorMode = EditorMode.MARKDOWN
                    }
                }
            }
            if (editorMode == EditorMode.VISUAL) {
                Text("按章节和层级直接填写，适合日常新增与修改。", color = Color.Gray, fontSize = 12.sp)
                VisualContentEditor(visualSections, onChange = { visualSections = it })
            } else {
                OutlinedTextField(
                    markdown,
                    { markdown = it },
                    Modifier.fillMaxWidth().height(300.dp),
                    label = { Text("文本内容") },
                    placeholder = { Text("# 导入新课\n- 一级要点\n    - 二级要点") },
                    shape = RoundedCornerShape(16.dp),
                )
            }
            if (module == ImportModule.TRIAL) {
                Surface(
                    onClick = { imagePicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .4f)),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (boardUri == null) "  上传板书 JPG / PNG" else "  板书图片已选择", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            GradientActionButton(
                text = if (editing) "保存修改" else "开始导入",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val effectiveMarkdown = if (editorMode == EditorMode.VISUAL) {
                        VisualEditorCodec.toMarkdown(visualSections)
                    } else {
                        markdown
                    }
                    when (module) {
                        ImportModule.TRIAL -> {
                            val parsed = DocumentParser.splitMarkdown(effectiveMarkdown, listOf("课程信息") + config.trialSectionNames)
                            val courseTitles = setOf("课程信息", "教学目标", "教学重点", "教学难点")
                            val courseParts = parsed.filter { section ->
                                courseTitles.any { section.title.contains(it) }
                            }
                            val body = parsed.filterNot { it in courseParts }.ifEmpty {
                                listOf(ContentSection(title = config.trialSectionNames.firstOrNull() ?: "正文", markdown = effectiveMarkdown))
                            }
                            val courseInfo = courseParts.joinToString("\n\n") { "# ${it.title}\n${it.markdown}" }
                                .ifBlank {
                                    buildString {
                                        appendLine("# 课程信息")
                                        appendLine(title)
                                        appendLine()
                                        appendLine("- 教材：$textbook")
                                        appendLine("- 单元：$unit")
                                        lessonOrder.toIntOrNull()?.takeIf { it > 0 }?.let { appendLine("- 课时：第${it}课") }
                                        appendLine("- 题材：$category")
                                    }.trim()
                                }
                            if (editingId == null) {
                                onAddTrial(title, textbook, unit, lessonOrder.toIntOrNull() ?: 0, category, courseInfo, body, boardUri, importance)
                            } else {
                                onUpdateTrial(editingId, title, textbook, unit, lessonOrder.toIntOrNull() ?: 0, category, courseInfo, body, boardUri, importance)
                            }
                        }
                        ImportModule.STRUCTURED -> {
                            val sections = DocumentParser.splitMarkdown(effectiveMarkdown, config.structuredSectionNames)
                            if (editingId == null) {
                                onAddStructured(category, title, sections, importance)
                            } else {
                                onUpdateStructured(editingId, category, title, sections, importance)
                            }
                        }
                        ImportModule.TEMPLATE -> {
                            if (editingId == null) {
                                onAddTemplate(category, title, effectiveMarkdown)
                            } else {
                                onUpdateTemplate(editingId, category, title, effectiveMarkdown)
                            }
                        }
                        ImportModule.BACKUP -> Unit
                    }
                    onComplete()
                },
            )
            Text(
                "默认使用分段编辑，也可切换到文本编辑进行批量排版。支持 .md、.txt、.docx。",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 30.dp),
            )
        }
    }
}

@Composable
private fun EditorModeCard(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFF5F4F5),
        contentColor = if (selected) Color.White else Color(0xFF55565D),
    ) {
        Text(title, modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ImportMethodCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .08f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE1E1E4)),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun titleLabel(module: ImportModule): String = when (module) {
    ImportModule.TRIAL -> "课程名称"
    ImportModule.STRUCTURED -> "结构化问题"
    ImportModule.TEMPLATE -> "模板名称"
    ImportModule.BACKUP -> "名称"
}

private fun buildSectionMarkdown(sections: List<ContentSection>): String {
    return sections.joinToString("\n\n") { "# ${it.title}\n${it.markdown}" }
}

private fun buildTrialMarkdown(lesson: com.shangan.teacherprep.data.TrialLesson): String {
    return listOf(
        lesson.courseInfoMarkdown,
        buildSectionMarkdown(lesson.bodySections),
    ).filter { it.isNotBlank() }.joinToString("\n\n")
}

private fun defaultSectionTitle(module: ImportModule, config: ScopeConfig): String = when (module) {
    ImportModule.TRIAL -> config.trialSectionNames.firstOrNull() ?: "导入新课"
    ImportModule.STRUCTURED -> config.structuredSectionNames.firstOrNull() ?: "答题思路"
    ImportModule.TEMPLATE -> "模板正文"
    ImportModule.BACKUP -> "正文"
}
