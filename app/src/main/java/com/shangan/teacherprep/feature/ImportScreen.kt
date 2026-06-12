package com.shangan.teacherprep.feature

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.ImportModule
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.ContentSection
import com.shangan.teacherprep.data.ScopeConfig
import com.shangan.teacherprep.ui.FilterChips
import com.shangan.teacherprep.ui.GradientActionButton
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScreenHeader
import com.shangan.teacherprep.util.DocumentParser

@Composable
fun ImportScreen(
    module: ImportModule,
    editingId: String?,
    data: AppData,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAddTrial: (String, String, String, String, List<ContentSection>, String?) -> Unit,
    onAddStructured: (String, String, List<ContentSection>) -> Unit,
    onAddTemplate: (String, String, String) -> Unit,
    onUpdateTrial: (String, String, String, String, String, List<ContentSection>, String?) -> Unit,
    onUpdateStructured: (String, String, String, List<ContentSection>) -> Unit,
    onUpdateTemplate: (String, String, String, String) -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val config = data.scopeConfigs[data.preferences.selectedScope.key] ?: ScopeConfig()
    val existingTrial = data.trials.firstOrNull { it.id == editingId }
    val existingStructured = data.structuredQuestions.firstOrNull { it.id == editingId }
    val existingTemplate = data.templates.firstOrNull { it.id == editingId }
    val editing = editingId != null
    var title by remember(editingId) {
        mutableStateOf(existingTrial?.title ?: existingStructured?.question ?: existingTemplate?.name.orEmpty())
    }
    var markdown by remember(editingId) {
        mutableStateOf(
            when {
                existingTrial != null -> buildTrialMarkdown(existingTrial)
                existingStructured != null -> buildSectionMarkdown(existingStructured.answerSections)
                existingTemplate != null -> existingTemplate.contentMarkdown
                else -> ""
            },
        )
    }
    var fileName by remember { mutableStateOf<String?>(null) }
    var textbook by remember(editingId) {
        mutableStateOf(existingTrial?.textbook ?: config.textbooks.firstOrNull().orEmpty())
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

    Column(modifier.fillMaxSize()) {
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
                ImportMethodCard("Markdown / 文档", Icons.Rounded.Description, fileName != null, Modifier.weight(1f)) {
                    documentPicker.launch(arrayOf("text/markdown", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                }
                ImportMethodCard("手动新建", Icons.Rounded.Edit, fileName == null, Modifier.weight(1f)) {
                    fileName = null
                    markdown = ""
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
            OutlinedTextField(
                markdown,
                { markdown = it },
                Modifier.fillMaxWidth().height(260.dp),
                label = { Text("Markdown / 正文") },
                placeholder = { Text("# 导入新课\n...\n# 整体感知\n...\n\n系统会按标题自动切分内容结构") },
                shape = RoundedCornerShape(16.dp),
            )
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
                    when (module) {
                        ImportModule.TRIAL -> {
                            val parsed = DocumentParser.splitMarkdown(markdown, listOf("课程信息") + config.trialSectionNames)
                            val courseTitles = setOf("课程信息", "教学目标", "教学重点", "教学难点")
                            val courseParts = parsed.filter { section ->
                                courseTitles.any { section.title.contains(it) }
                            }
                            val body = parsed.filterNot { it in courseParts }.ifEmpty {
                                listOf(ContentSection(title = config.trialSectionNames.firstOrNull() ?: "正文", markdown = markdown))
                            }
                            val courseInfo = courseParts.joinToString("\n\n") { "# ${it.title}\n${it.markdown}" }
                                .ifBlank { "# 课程信息\n$title\n\n- 教材：$textbook\n- 题材：$category" }
                            if (editingId == null) {
                                onAddTrial(title, textbook, category, courseInfo, body, boardUri)
                            } else {
                                onUpdateTrial(editingId, title, textbook, category, courseInfo, body, boardUri)
                            }
                        }
                        ImportModule.STRUCTURED -> {
                            val sections = DocumentParser.splitMarkdown(markdown, config.structuredSectionNames)
                            if (editingId == null) {
                                onAddStructured(category, title, sections)
                            } else {
                                onUpdateStructured(editingId, category, title, sections)
                            }
                        }
                        ImportModule.TEMPLATE -> {
                            if (editingId == null) {
                                onAddTemplate(category, title, markdown)
                            } else {
                                onUpdateTemplate(editingId, category, title, markdown)
                            }
                        }
                        ImportModule.BACKUP -> Unit
                    }
                    onComplete()
                },
            )
            Text(
                "支持 .md、.txt、.docx；一级到六级标题会自动切分为可维护的内容段落。",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 30.dp),
            )
        }
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
