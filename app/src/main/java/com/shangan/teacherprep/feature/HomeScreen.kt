package com.shangan.teacherprep.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.ui.MainDestination
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScopePill
import com.shangan.teacherprep.ui.theme.LocalPrepColors

@Composable
fun HomeScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onNavigate: (MainDestination) -> Unit,
    onSwitchScope: () -> Unit,
    onRandomTrial: () -> Unit,
) {
    val scope = data.preferences.selectedScope
    val key = scope.key
    val trials = data.trials.filter { it.scopeKey == key }
    val structured = data.structuredQuestions.filter { it.scopeKey == key }
    val templates = data.templates.filter { it.scopeKey == key }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 26.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("上午好", fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text("今天想练什么？", color = Color.Gray, fontSize = 18.sp)
                }
                ScopePill(scope, onSwitchScope)
            }
        }
        item {
            Surface(
                onClick = onRandomTrial,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 8.dp,
                color = Color.Transparent,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(210.dp)
                        .background(Brush.linearGradient(listOf(LocalPrepColors.current.primary, LocalPrepColors.current.secondary))),
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("今日练习", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                        Text("随机抽一道，开始模拟", color = Color.White.copy(alpha = .9f), fontSize = 17.sp)
                        Spacer(Modifier.height(28.dp))
                        Surface(shape = RoundedCornerShape(50), color = Color.White) {
                            Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = LocalPrepColors.current.primary)
                                Text("开始抽题", color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = .18f),
                        modifier = Modifier.size(145.dp).align(Alignment.CenterEnd).padding(12.dp),
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleCard("试讲", "按教材与题材查找", trials.size, Icons.Rounded.Slideshow, LocalPrepColors.current.primary, Modifier.weight(1f)) {
                    onNavigate(MainDestination.TRIAL)
                }
                ModuleCard("结构化", "按问题种类查找", structured.size, Icons.Rounded.ChatBubbleOutline, Color(0xFF7654F6), Modifier.weight(1f)) {
                    onNavigate(MainDestination.STRUCTURED)
                }
                ModuleCard("模板", "高频答题框架", templates.size, Icons.Rounded.Layers, Color(0xFFFF9418), Modifier.weight(1f)) {
                    onNavigate(MainDestination.TEMPLATE)
                }
            }
        }
        item { Text("最近练习", fontSize = 23.sp, fontWeight = FontWeight.Black) }
        items(minOf(trials.size, 3)) { index ->
            val item = trials[index]
            RoundedCard(onClick = { onNavigate(MainDestination.TRIAL) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).background(LocalPrepColors.current.primary.copy(alpha = .1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Slideshow, null, tint = LocalPrepColors.current.primary)
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${item.textbook} · ${item.genre}", color = Color.Gray, fontSize = 13.sp)
                    }
                    Text("${(item.progress * 100).toInt()}%", color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier.height(180.dp), shape = RoundedCornerShape(22.dp), color = color, shadowElevation = 5.dp) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
            Text(subtitle, color = Color.White.copy(alpha = .85f), fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.weight(1f))
            Icon(icon, null, tint = Color.White.copy(alpha = .45f), modifier = Modifier.size(55.dp).align(Alignment.CenterHorizontally))
            Surface(shape = CircleShape, color = Color.White) {
                Text("$count 条", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
