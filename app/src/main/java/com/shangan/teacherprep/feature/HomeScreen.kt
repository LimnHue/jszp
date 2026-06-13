package com.shangan.teacherprep.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.ui.RandomDrawCandidate
import com.shangan.teacherprep.ui.RandomDrawDialog
import com.shangan.teacherprep.ui.RandomDrawGroup
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScopePill
import com.shangan.teacherprep.ui.practiceHistoryText
import com.shangan.teacherprep.ui.theme.LocalPrepColors
import java.time.LocalTime

@Composable
fun HomeScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onNavigate: (MainDestination) -> Unit,
    onSwitchScope: () -> Unit,
    onRandomTrial: (String) -> Unit,
    onRandomStructured: (String) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val scope = data.preferences.selectedScope
    val key = scope.key
    val trials = data.trials.filter { it.scopeKey == key }
    val structured = data.structuredQuestions.filter { it.scopeKey == key }
    val templates = data.templates.filter { it.scopeKey == key }
    var drawModule by remember { mutableStateOf<MainDestination?>(null) }
    val recentPractices = (
        trials.map {
            RecentPractice(
                title = it.title,
                subtitle = "试讲 · ${it.textbook} · ${it.genre}",
                practiceCount = it.practiceCount,
                lastPracticedAt = it.lastPracticedAt,
                destination = MainDestination.TRIAL,
                icon = Icons.Rounded.Slideshow,
                color = LocalPrepColors.current.primary,
            )
        } + structured.map {
            RecentPractice(
                title = it.question,
                subtitle = "结构化 · ${it.category}",
                practiceCount = it.practiceCount,
                lastPracticedAt = it.lastPracticedAt,
                destination = MainDestination.STRUCTURED,
                icon = Icons.Rounded.ChatBubbleOutline,
                color = Color(0xFF7654F6),
            )
        } + templates.map {
            RecentPractice(
                title = it.name,
                subtitle = "模板 · ${it.category}",
                practiceCount = it.practiceCount,
                lastPracticedAt = it.lastPracticedAt,
                destination = MainDestination.TEMPLATE,
                icon = Icons.Rounded.Layers,
                color = Color(0xFFFF9418),
            )
        }
    ).filter { it.lastPracticedAt != null }
        .sortedByDescending { it.lastPracticedAt }
        .take(5)

    val listState = rememberLazyListState()
    Box(
        Modifier.fillMaxSize(),
    ) {
        LazyColumn(
        state = listState,
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
                    Text(greetingForHour(LocalTime.now().hour), fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text("今天想练什么？", color = Color.Gray, fontSize = 18.sp)
                }
                ScopePill(scope, onSwitchScope)
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 8.dp,
                color = Color.Transparent,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(174.dp)
                        .background(Brush.linearGradient(listOf(LocalPrepColors.current.primary, LocalPrepColors.current.secondary))),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("今日练习", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("选择一种题型，开始模拟", color = Color.White.copy(alpha = .9f), fontSize = 15.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                onClick = { drawModule = MainDestination.TRIAL },
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                            ) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Slideshow, null, tint = LocalPrepColors.current.primary)
                                    Text(" 抽试讲", color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Surface(
                                onClick = { drawModule = MainDestination.STRUCTURED },
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                            ) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color(0xFF7654F6))
                                    Text(" 抽结构化", color = Color(0xFF7654F6), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = .18f),
                        modifier = Modifier.size(112.dp).align(Alignment.CenterEnd).padding(12.dp),
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleCard("试讲", "按教材、单元与题材查找", trials.size, Icons.Rounded.Slideshow, LocalPrepColors.current.primary, Modifier.fillMaxWidth()) {
                    onNavigate(MainDestination.TRIAL)
                }
                ModuleCard("结构化", "按问题种类与重要程度查找", structured.size, Icons.Rounded.ChatBubbleOutline, Color(0xFF7654F6), Modifier.fillMaxWidth()) {
                    onNavigate(MainDestination.STRUCTURED)
                }
                ModuleCard("模板", "查看高频答题框架", templates.size, Icons.Rounded.Layers, Color(0xFFFF9418), Modifier.fillMaxWidth()) {
                    onNavigate(MainDestination.TEMPLATE)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("最近练习", fontSize = 23.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Surface(onClick = onOpenCalendar, shape = RoundedCornerShape(50), color = LocalPrepColors.current.primary.copy(alpha = .1f)) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(18.dp))
                        Text(" 日历", color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (recentPractices.isEmpty()) {
            item {
                RoundedCard(containerColor = Color(0xFFF7F8FB)) {
                    Text("还没有练习记录，开始一次计时练习后会显示在这里。", color = Color.Gray)
                }
            }
        } else {
            items(recentPractices) { item ->
                RoundedCard(onClick = { onNavigate(item.destination) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(58.dp).background(item.color.copy(alpha = .1f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, null, tint = item.color)
                        }
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(item.subtitle, color = Color.Gray, fontSize = 13.sp)
                            Text(
                                practiceHistoryText(item.practiceCount, item.lastPracticedAt),
                                color = item.color,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        }
        DraggableScrollToTopButton(
            listState,
            Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        )
    }

    when (drawModule) {
        MainDestination.TRIAL -> RandomDrawDialog(
            title = "按标签抽试讲",
            groups = listOf(
                RandomDrawGroup("textbook", "教材", trials.map { it.textbook }.distinct()),
                RandomDrawGroup("unit", "单元", trials.map { it.unit }.filter { it.isNotBlank() }.distinct()),
                RandomDrawGroup("genre", "题材", trials.map { it.genre }.distinct()),
                RandomDrawGroup("importance", "重要程度", trials.map { "${it.importance} 星" }.distinct().sortedDescending()),
            ),
            candidates = trials.map {
                RandomDrawCandidate(
                    it.id,
                    mapOf(
                        "textbook" to it.textbook,
                        "unit" to it.unit,
                        "genre" to it.genre,
                        "importance" to "${it.importance} 星",
                    ),
                )
            },
            onDismiss = { drawModule = null },
            onDraw = {
                drawModule = null
                onRandomTrial(it)
            },
        )
        MainDestination.STRUCTURED -> RandomDrawDialog(
            title = "按标签抽结构化",
            groups = listOf(
                RandomDrawGroup("category", "问题种类", structured.map { it.category }.distinct()),
                RandomDrawGroup("importance", "重要程度", structured.map { "${it.importance} 星" }.distinct().sortedDescending()),
            ),
            candidates = structured.map {
                RandomDrawCandidate(
                    it.id,
                    mapOf("category" to it.category, "importance" to "${it.importance} 星"),
                )
            },
            onDismiss = { drawModule = null },
            onDraw = {
                drawModule = null
                onRandomStructured(it)
            },
            accent = Color(0xFF7654F6),
        )
        else -> Unit
    }
}

internal fun greetingForHour(hour: Int): String = when (hour) {
    in 5..10 -> "上午好"
    in 11..12 -> "中午好"
    in 13..17 -> "下午好"
    in 18..23 -> "晚上好"
    else -> "夜深了"
}

private data class RecentPractice(
    val title: String,
    val subtitle: String,
    val practiceCount: Int,
    val lastPracticedAt: Long?,
    val destination: MainDestination,
    val icon: ImageVector,
    val color: Color,
)

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
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .22f)),
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(color.copy(alpha = .11f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.padding(start = 13.dp).weight(1f)) {
                Text(title, color = Color(0xFF24242A), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Surface(shape = CircleShape, color = color.copy(alpha = .1f)) {
                Text("$count 条", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
