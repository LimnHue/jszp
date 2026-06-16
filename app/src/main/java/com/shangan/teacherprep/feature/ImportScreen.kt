package com.shangan.teacherprep.feature

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.shangan.teacherprep.data.StructuredImportItem
import com.shangan.teacherprep.data.TrialImportItem
import com.shangan.teacherprep.ui.FilterChips
import com.shangan.teacherprep.ui.GradientActionButton
import com.shangan.teacherprep.ui.ImportanceStars
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScreenHeader
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.util.BatchDocumentItem
import com.shangan.teacherprep.util.BatchImportParser
import com.shangan.teacherprep.util.DocumentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class EditorMode { VISUAL, MARKDOWN }

private data class ParsedImportDocument(
    val markdown: String,
    val visualSections: List<VisualSection>,
    val batchItems: List<BatchDocumentItem>,
)

@Composable
fun ImportScreen(
    module: ImportModule,
    editingId: String?,
    data: AppData,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAddTrial: (String, String, String, Int, String, String, List<ContentSection>, String?, Int) -> Unit,
    onAddTrialBatch: (String, String, Int, String, List<TrialImportItem>, String?, Int) -> Unit,
    onAddStructured: (String, String, List<ContentSection>, Int) -> Unit,
    onAddStructuredBatch: (String, List<StructuredImportItem>, Int) -> Unit,
    onAddTemplate: (String, String, String) -> Unit,
    onUpdateTrial: (String, String, String, String, Int, String, String, List<ContentSection>, String?, Int) -> Unit,
    onUpdateStructured: (String, String, String, List<ContentSection>, Int) -> Unit,
    onUpdateTemplate: (String, String, String, String) -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val importScope = rememberCoroutineScope()
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
    var batchItems by remember(editingId, module) { mutableStateOf<List<BatchDocumentItem>>(emptyList()) }
    var isImportingFile by remember(editingId, module) { mutableStateOf(false) }
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
            markdown = ""
            visualSections = listOf(VisualSection(title = defaultSectionTitle(module, config)))
            batchItems = emptyList()
            isImportingFile = true
            val appContext = context.applicationContext
            importScope.launch {
                val result = runCatching {
                    val parsedText = withContext(Dispatchers.IO) {
                        DocumentParser.readText(appContext, it, name)
                    }
                    val parsedSections = withContext(Dispatchers.Default) {
                        VisualEditorCodec.parse(parsedText, defaultSectionTitle(module, config))
                    }
                    val parsedBatchItems = withContext(Dispatchers.Default) {
                        if (editing) emptyList() else parseBatchItems(module, parsedText)
                    }
                    ParsedImportDocument(parsedText, parsedSections, parsedBatchItems)
                }
                result.onSuccess { parsed ->
                    markdown = parsed.markdown
                    visualSections = parsed.visualSections
                    batchItems = parsed.batchItems
                }.onFailure { error ->
                    markdown = "读取失败：${error.message ?: "文件解析异常"}"
                    visualSections = VisualEditorCodec.parse(markdown, defaultSectionTitle(module, config))
                    batchItems = emptyList()
                }
                isImportingFile = false
            }
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
    val formScrollState = rememberScrollState()

    Box(
        modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .025f)),
    ) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(if (editing) "修改内容" else "导入内容", onBack = onBack)
            Column(
            Modifier.weight(1f).verticalScroll(formScrollState).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (editing) "修改后将直接更新原内容" else "把你的资料整理进专属题库",
                color = Color.Gray,
                fontSize = 17.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ImportMethodCard("导入文档", Icons.Rounded.Description, fileName != null, Modifier.weight(1f)) {
                    documentPicker.launch(arrayOf("text/markdown", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf"))
                }
                ImportMethodCard("手动新建", Icons.Rounded.Edit, fileName == null, Modifier.weight(1f)) {
                    fileName = null
                    markdown = ""
                    batchItems = emptyList()
                    isImportingFile = false
                    visualSections = listOf(VisualSection(title = defaultSectionTitle(module, config)))
                }
            }
            if (isImportingFile) {
                RoundedCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp),
                            strokeWidth = 3.dp,
                        )
                        Text("正在解析文件，请稍等", fontWeight = FontWeight.Bold)
                    }
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
            if (!editing && batchItems.size > 1 && (module == ImportModule.TRIAL || module == ImportModule.STRUCTURED)) {
                BatchImportPreview(
                    module = module,
                    items = batchItems,
                    onImport = {
                        when (module) {
                            ImportModule.TRIAL -> {
                                val drafts = batchItems.map { item ->
                                    buildTrialImportItem(item, title, textbook, unit, lessonOrder.toIntOrNull() ?: 0, category, config)
                                }
                                onAddTrialBatch(textbook, unit, lessonOrder.toIntOrNull() ?: 0, category, drafts, boardUri, importance)
                            }
                            ImportModule.STRUCTURED -> {
                                val drafts = batchItems.map { item ->
                                    StructuredImportItem(
                                        question = item.title,
                                        answerSections = listOf(
                                            ContentSection(
                                                title = config.structuredSectionNames.lastOrNull() ?: "参考答案",
                                                markdown = item.markdown,
                                            ),
                                        ),
                                    )
                                }
                                onAddStructuredBatch(category, drafts, importance)
                            }
                            else -> Unit
                        }
                        onComplete()
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            GradientActionButton(
                text = if (editing) "保存修改" else "开始导入",
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImportingFile,
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
                "默认使用分段编辑，也可切换到文本编辑进行批量排版。支持 .md、.txt、.docx、.pdf。",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 30.dp),
            )
            }
        }
        DraggableScrollToTopButton(formScrollState)
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

@Composable
private fun BatchImportPreview(
    module: ImportModule,
    items: List<BatchDocumentItem>,
    onImport: () -> Unit,
) {
    RoundedCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "已识别 ${items.size} 条${if (module == ImportModule.TRIAL) "试讲" else "结构化"}内容",
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            items.take(5).forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. ${item.title}",
                    color = Color(0xFF55565D),
                    fontSize = 13.sp,
                    maxLines = 2,
                )
            }
            if (items.size > 5) {
                Text("还有 ${items.size - 5} 条将一起导入", color = Color.Gray, fontSize = 12.sp)
            }
            GradientActionButton(
                text = "批量导入 ${items.size} 条",
                modifier = Modifier.fillMaxWidth(),
                onClick = onImport,
            )
        }
    }
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

private fun parseBatchItems(module: ImportModule, markdown: String): List<BatchDocumentItem> {
    return when (module) {
        ImportModule.TRIAL -> BatchImportParser.parseTrial(markdown)
        ImportModule.STRUCTURED -> BatchImportParser.parseStructured(markdown)
        else -> emptyList()
    }
}

private fun buildTrialImportItem(
    item: BatchDocumentItem,
    fallbackTitle: String,
    textbook: String,
    unit: String,
    lessonOrder: Int,
    category: String,
    config: ScopeConfig,
): TrialImportItem {
    val parsed = DocumentParser.splitMarkdown(item.markdown, listOf("课程信息") + config.trialSectionNames)
    val courseTitles = setOf("课程信息", "教学目标", "教学重点", "教学难点")
    val courseParts = parsed.filter { section ->
        courseTitles.any { section.title.contains(it) }
    }
    val body = parsed.filterNot { it in courseParts }.ifEmpty {
        listOf(ContentSection(title = config.trialSectionNames.firstOrNull() ?: "正文", markdown = item.markdown))
    }
    val courseInfo = courseParts.joinToString("\n\n") { "# ${it.title}\n${it.markdown}" }
        .ifBlank {
            buildString {
                appendLine("# 课程信息")
                appendLine(item.title.ifBlank { fallbackTitle })
                appendLine()
                appendLine("- 教材：$textbook")
                appendLine("- 单元：$unit")
                if (lessonOrder > 0) appendLine("- 课时：第${lessonOrder}课")
                appendLine("- 题材：$category")
            }.trim()
        }
    return TrialImportItem(
        title = item.title.ifBlank { fallbackTitle },
        courseInfoMarkdown = courseInfo,
        bodySections = body,
    )
}
