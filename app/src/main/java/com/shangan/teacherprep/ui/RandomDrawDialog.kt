package com.shangan.teacherprep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.ui.theme.LocalPrepColors

data class RandomDrawGroup(
    val key: String,
    val label: String,
    val options: List<String>,
)

data class RandomDrawCandidate(
    val id: String,
    val tags: Map<String, String>,
)

@Composable
fun RandomDrawDialog(
    title: String,
    groups: List<RandomDrawGroup>,
    candidates: List<RandomDrawCandidate>,
    onDismiss: () -> Unit,
    onDraw: (String) -> Unit,
    initialSelections: Map<String, Set<String>> = emptyMap(),
    accent: Color = LocalPrepColors.current.primary,
) {
    val selected: SnapshotStateMap<String, Set<String>> = remember(title, candidates) {
        groups.map { group ->
            group.key to initialSelections[group.key].orEmpty().filter { it in group.options }.toSet()
        }.toMutableStateMap()
    }
    val available = candidates.filter { candidate ->
        groups.all { group ->
            val values = selected[group.key].orEmpty()
            values.isEmpty() || candidate.tags[group.key] in values
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shuffle, null, tint = accent)
                Text("  $title", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("选择标签限定抽取范围；同组多选取并集，不同组之间取交集。", color = Color.Gray, fontSize = 13.sp)
                groups.filter { it.options.isNotEmpty() }.forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val groupSelection = selected[group.key].orEmpty()
                            item {
                                DrawTag(
                                    text = "不限",
                                    selected = groupSelection.isEmpty(),
                                    accent = accent,
                                    onClick = { selected[group.key] = emptySet() },
                                )
                            }
                            items(group.options.distinct()) { option ->
                                DrawTag(
                                    text = option,
                                    selected = option in groupSelection,
                                    accent = accent,
                                    onClick = {
                                        selected[group.key] = if (option in groupSelection) {
                                            groupSelection - option
                                        } else {
                                            groupSelection + option
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                Surface(
                    color = accent.copy(alpha = .09f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (available.isEmpty()) "当前标签下没有可抽取内容" else "当前范围：${available.size} 条",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        confirmButton = {
            TextButton(
                enabled = available.isNotEmpty(),
                onClick = { available.randomOrNull()?.let { onDraw(it.id) } },
            ) {
                Text("随机抽取", fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun DrawTag(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent else Color(0xFFF4F3F5),
        contentColor = if (selected) Color.White else Color(0xFF55565D),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}
