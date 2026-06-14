package com.shangan.teacherprep.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.PracticeModule
import com.shangan.teacherprep.ui.MainDestination
import com.shangan.teacherprep.ui.BrandMark
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.ui.RandomDrawCandidate
import com.shangan.teacherprep.ui.RandomDrawDialog
import com.shangan.teacherprep.ui.RandomDrawGroup
import com.shangan.teacherprep.ui.randomDrawId
import com.shangan.teacherprep.ui.savedRandomDrawSelections
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
    onUpdateDrawSelections: (PracticeModule, Map<String, Set<String>>) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val scope = data.preferences.selectedScope
    val key = scope.key
    val trials = data.trials.filter { it.scopeKey == key }
    val structured = data.structuredQuestions.filter { it.scopeKey == key }
    val templates = data.templates.filter { it.scopeKey == key }
    var drawModule by remember { mutableStateOf<MainDestination?>(null) }
    val trialDrawGroups = listOf(
        RandomDrawGroup("textbook", "教材", trials.map { it.textbook }.distinct()),
        RandomDrawGroup("unit", "单元", trials.map { it.unit }.filter { it.isNotBlank() }.distinct()),
        RandomDrawGroup("genre", "题材", trials.map { it.genre }.distinct()),
        RandomDrawGroup("importance", "重要程度", trials.map { "${it.importance} 星" }.distinct().sortedDescending()),
    )
    val trialDrawCandidates = trials.map {
        RandomDrawCandidate(
            it.id,
            mapOf(
                "textbook" to it.textbook,
                "unit" to it.unit,
                "genre" to it.genre,
                "importance" to "${it.importance} 星",
            ),
        )
    }
    val structuredDrawGroups = listOf(
        RandomDrawGroup("category", "问题种类", structured.map { it.category }.distinct()),
        RandomDrawGroup("importance", "重要程度", structured.map { "${it.importance} 星" }.distinct().sortedDescending()),
    )
    val structuredDrawCandidates = structured.map {
        RandomDrawCandidate(
            it.id,
            mapOf("category" to it.category, "importance" to "${it.importance} 星"),
        )
    }
    val trialDrawSelections = savedRandomDrawSelections(data.preferences, key, PracticeModule.TRIAL)
    val structuredDrawSelections = savedRandomDrawSelections(data.preferences, key, PracticeModule.STRUCTURED)
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
                color = LocalPrepColors.current.primary,
            )
        } + templates.map {
            RecentPractice(
                title = it.name,
                subtitle = "模板 · ${it.category}",
                practiceCount = it.practiceCount,
                lastPracticedAt = it.lastPracticedAt,
                destination = MainDestination.TEMPLATE,
                icon = Icons.Rounded.Layers,
                color = LocalPrepColors.current.primary,
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
            top = contentPadding.calculateTopPadding() + 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 42)
                Column(Modifier.padding(start = 13.dp).weight(1f)) {
                    Text(greetingForHour(LocalTime.now().hour), fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("今天想练什么？", color = Color(0xFF74777A), fontSize = 15.sp)
                }
                ScopePill(scope, onSwitchScope)
            }
        }
        item {
            Text("今日练习", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFCFBF8),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    QuickDrawControl(
                        label = "抽试讲",
                        subtitle = "从已设范围随机抽取一课",
                        icon = Icons.Rounded.Slideshow,
                        useBrandMark = true,
                        onDraw = { randomDrawId(trialDrawGroups, trialDrawCandidates, trialDrawSelections)?.let(onRandomTrial) },
                        onSettings = { drawModule = MainDestination.TRIAL },
                    )
                    HorizontalDivider(color = Color(0xFFE4E1DC))
                    QuickDrawControl(
                        label = "抽结构化",
                        subtitle = "从已设范围随机抽取一个问题",
                        icon = Icons.Rounded.ChatBubbleOutline,
                        onDraw = { randomDrawId(structuredDrawGroups, structuredDrawCandidates, structuredDrawSelections)?.let(onRandomStructured) },
                        onSettings = { drawModule = MainDestination.STRUCTURED },
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFCFBF8),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                ModuleCard("试讲", "按教材、单元与题材查找", trials.size, Icons.Rounded.Slideshow, Modifier.fillMaxWidth(), useBrandMark = true) {
                    onNavigate(MainDestination.TRIAL)
                }
                HorizontalDivider(color = Color(0xFFE4E1DC))
                ModuleCard("结构化", "按问题种类与重要程度查找", structured.size, Icons.Rounded.ChatBubbleOutline, Modifier.fillMaxWidth()) {
                    onNavigate(MainDestination.STRUCTURED)
                }
                HorizontalDivider(color = Color(0xFFE4E1DC))
                ModuleCard("模板", "查看高频答题框架", templates.size, Icons.Rounded.Layers, Modifier.fillMaxWidth()) {
                    onNavigate(MainDestination.TEMPLATE)
                }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("最近练习", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Surface(
                    onClick = onOpenCalendar,
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFCFBF8),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
                ) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(18.dp))
                        Text(" 日历", color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (recentPractices.isEmpty()) {
            item {
                RoundedCard(containerColor = Color(0xFFFCFBF8)) {
                    Text("还没有练习记录，开始一次计时练习后会显示在这里。", color = Color.Gray)
                }
            }
        } else {
            items(recentPractices) { item ->
                RoundedCard(onClick = { onNavigate(item.destination) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(54.dp).background(Color(0xFFE9EEF2), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.destination == MainDestination.TRIAL) {
                                BrandMark(size = 32)
                            } else {
                                Icon(item.icon, null, tint = LocalPrepColors.current.primary)
                            }
                        }
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(item.subtitle, color = Color.Gray, fontSize = 13.sp)
                            Text(
                                practiceHistoryText(item.practiceCount, item.lastPracticedAt),
                                color = LocalPrepColors.current.primary,
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
            groups = trialDrawGroups,
            candidates = trialDrawCandidates,
            initialSelections = trialDrawSelections,
            onDismiss = { drawModule = null },
            onSave = {
                onUpdateDrawSelections(PracticeModule.TRIAL, it)
                drawModule = null
            },
        )
        MainDestination.STRUCTURED -> RandomDrawDialog(
            title = "按标签抽结构化",
            groups = structuredDrawGroups,
            candidates = structuredDrawCandidates,
            initialSelections = structuredDrawSelections,
            onDismiss = { drawModule = null },
            onSave = {
                onUpdateDrawSelections(PracticeModule.STRUCTURED, it)
                drawModule = null
            },
            accent = LocalPrepColors.current.primary,
        )
        else -> Unit
    }
}

@Composable
private fun QuickDrawControl(
    label: String,
    subtitle: String,
    icon: ImageVector,
    useBrandMark: Boolean = false,
    onDraw: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(78.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onDraw).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(46.dp).background(Color(0xFFE9EEF2), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (useBrandMark) {
                    BrandMark(size = 30)
                } else {
                    Icon(icon, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(25.dp))
                }
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(label, color = Color(0xFF202224), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(subtitle, color = Color(0xFF74777A), fontSize = 12.sp)
            }
        }
        HorizontalDivider(Modifier.height(42.dp).width(1.dp), color = Color(0xFFE4E1DC))
        Surface(onClick = onSettings, shape = CircleShape, color = Color.Transparent) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "设置${label}范围",
                tint = LocalPrepColors.current.primary,
                modifier = Modifier.padding(14.dp).size(24.dp),
            )
        }
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
    modifier: Modifier,
    useBrandMark: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(Color(0xFFE9EEF2), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (useBrandMark) {
                    BrandMark(size = 30)
                } else {
                    Icon(icon, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(25.dp))
                }
            }
            Column(Modifier.padding(start = 13.dp).weight(1f)) {
                Text(title, color = Color(0xFF24242A), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
            ) {
                Text("$count 条", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color(0xFF202224), fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF74777A), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
