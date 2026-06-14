package com.shangan.teacherprep.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.LibraryScope
import com.shangan.teacherprep.ui.GradientActionButton
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.ui.BrandMark
import com.shangan.teacherprep.ui.theme.LocalPrepColors

@Composable
fun LibrarySelectionScreen(
    data: AppData,
    modifier: Modifier = Modifier,
    onAddStage: (String) -> Unit,
    onAddSubject: (String) -> Unit,
    onAddTextbookVersion: (String) -> Unit,
    onEnter: (LibraryScope) -> Unit,
) {
    var stage by remember { mutableStateOf(data.preferences.selectedScope.stage) }
    var subject by remember { mutableStateOf(data.preferences.selectedScope.subject) }
    var textbookVersion by remember { mutableStateOf(data.preferences.selectedScope.textbookVersion) }
    var adding by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF7F5F1))) {
        Column(Modifier.fillMaxSize()) {
            Column(
            modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 46)
                Text("  教招上岸", fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(26.dp))
            Text("选择你的备考题库", fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black)
            Text("以后可以随时切换，每个组合都是独立资料库", color = Color.Gray, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(30.dp))

            SelectionPanel("学段", data.preferences.stages, stage, { stage = it }, { adding = "学段" })
            Spacer(Modifier.height(16.dp))
            SelectionPanel("学科", data.preferences.subjects, subject, { subject = it }, { adding = "学科" })
            Spacer(Modifier.height(16.dp))
            SelectionPanel(
                "教材版本",
                data.preferences.textbookVersions,
                textbookVersion,
                { textbookVersion = it },
                { adding = "教材版本" },
            )
            Spacer(Modifier.height(24.dp))
        }
            Surface(color = Color.White.copy(alpha = .97f), shadowElevation = 8.dp) {
                GradientActionButton(
                    text = "进入题库",
                    onClick = { onEnter(LibraryScope(stage, subject, textbookVersion)) },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
        DraggableScrollToTopButton(scrollState, Modifier.padding(bottom = 78.dp))
    }

    adding?.let { type ->
        AddOptionDialog(
            title = "新增$type",
            onDismiss = { adding = null },
            onConfirm = {
                if (type == "学段") {
                    onAddStage(it)
                    stage = it
                } else if (type == "学科") {
                    onAddSubject(it)
                    subject = it
                } else {
                    onAddTextbookVersion(it)
                    textbookVersion = it
                }
                adding = null
            },
        )
    }
}

@Composable
private fun SelectionPanel(
    title: String,
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = .96f),
        shadowElevation = 5.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("自定义")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                values.chunked(3).forEach { rowValues ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowValues.forEach { value ->
                            SelectionOption(
                                title = title,
                                value = value,
                                active = value == selected,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelect(value) },
                            )
                        }
                        repeat(3 - rowValues.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionOption(
    title: String,
    value: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (active) LocalPrepColors.current.primary.copy(alpha = .09f) else Color(0xFFF7F7F8),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) LocalPrepColors.current.primary else Color(0xFFE8E8EA),
        ),
    ) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (title == "学段") Icons.Rounded.School else Icons.Rounded.MenuBook,
                    contentDescription = null,
                    tint = if (active) LocalPrepColors.current.primary else Color.Gray,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    " $value",
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 20.sp,
                )
                if (active) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = LocalPrepColors.current.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddOptionDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, label = { Text("名称") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
