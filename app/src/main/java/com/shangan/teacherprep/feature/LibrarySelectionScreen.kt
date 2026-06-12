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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.shangan.teacherprep.ui.theme.LocalPrepColors

@Composable
fun LibrarySelectionScreen(
    data: AppData,
    modifier: Modifier = Modifier,
    onAddStage: (String) -> Unit,
    onAddSubject: (String) -> Unit,
    onEnter: (LibraryScope) -> Unit,
) {
    var stage by remember { mutableStateOf(data.preferences.selectedScope.stage) }
    var subject by remember { mutableStateOf(data.preferences.selectedScope.subject) }
    var adding by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(LocalPrepColors.current.primary.copy(alpha = .1f), Color.White, Color.White)),
        ).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(54.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(LocalPrepColors.current.primary, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = Color.White)
            }
            Text("  上岸备课", fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(26.dp))
        Text("选择你的备考题库", fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black)
        Text("以后可以随时切换，每个组合都是独立资料库", color = Color.Gray, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(30.dp))

        SelectionPanel("学段", data.preferences.stages, stage, { stage = it }, { adding = "学段" })
        Spacer(Modifier.height(16.dp))
        SelectionPanel("学科", data.preferences.subjects, subject, { subject = it }, { adding = "学科" })
        Spacer(Modifier.weight(1f))
        GradientActionButton(
            text = "进入题库",
            onClick = { onEnter(LibraryScope(stage, subject)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
        )
    }

    adding?.let { type ->
        AddOptionDialog(
            title = "新增$type",
            onDismiss = { adding = null },
            onConfirm = {
                if (type == "学段") {
                    onAddStage(it)
                    stage = it
                } else {
                    onAddSubject(it)
                    subject = it
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(if (values.size > 6) 190.dp else 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(values) { value ->
                    val active = value == selected
                    Surface(
                        onClick = { onSelect(value) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) LocalPrepColors.current.primary.copy(alpha = .09f) else Color(0xFFF7F7F8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) LocalPrepColors.current.primary else Color(0xFFE8E8EA)),
                    ) {
                        Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (title == "学段") Icons.Rounded.School else Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = if (active) LocalPrepColors.current.primary else Color.Gray,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(" $value", fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                if (active) Icon(Icons.Rounded.CheckCircle, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
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
