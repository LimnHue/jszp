package com.shangan.teacherprep.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.PracticeEvent
import com.shangan.teacherprep.data.PracticeModule
import com.shangan.teacherprep.ui.ScreenHeader
import com.shangan.teacherprep.ui.theme.LocalPrepColors
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val calendarTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun PracticeCalendarScreen(
    data: AppData,
    onBack: () -> Unit,
    onOpenEvent: (PracticeEvent) -> Unit,
) {
    val scopeKey = data.preferences.selectedScope.key
    val zone = ZoneId.systemDefault()
    val events = data.practiceEvents.filter { it.scopeKey == scopeKey }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val eventsByDate = events.groupBy {
        Instant.ofEpochMilli(it.practicedAt).atZone(zone).toLocalDate()
    }
    val selectedEvents = eventsByDate[selectedDate].orEmpty().sortedByDescending { it.practicedAt }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("练习日历", onBack = onBack) }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF2F7FF),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            month = month.minusMonths(1)
                            selectedDate = month.atDay(1)
                        }) { Icon(Icons.Rounded.ChevronLeft, "上个月") }
                        Text(
                            "${month.year}年${month.monthValue}月",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                            fontSize = 21.sp,
                        )
                        IconButton(onClick = {
                            month = month.plusMonths(1)
                            selectedDate = month.atDay(1)
                        }) { Icon(Icons.Rounded.ChevronRight, "下个月") }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                    val offset = month.atDay(1).dayOfWeek.value - 1
                    val cells = List(offset) { null } + (1..month.lengthOfMonth()).map(month::atDay)
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            (week + List(7 - week.size) { null }).forEach { date ->
                                CalendarDay(
                                    date = date,
                                    count = date?.let { eventsByDate[it]?.size } ?: 0,
                                    selected = date == selectedDate,
                                    onClick = { if (date != null) selectedDate = date },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            val countText = if (selectedEvents.isEmpty()) "没有练习" else "练习 ${selectedEvents.size} 次"
            Text(
                "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 · $countText",
                modifier = Modifier.padding(horizontal = 20.dp),
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
            )
        }
        if (selectedEvents.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF7F8FA),
                ) {
                    Text("当天还没有练习记录", Modifier.padding(18.dp), color = Color.Gray)
                }
            }
        } else {
            items(selectedEvents) { event ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onOpenEvent(event) },
                    shape = RoundedCornerShape(18.dp),
                    color = moduleColor(event.module).copy(alpha = .09f),
                ) {
                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(event.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                "${moduleLabel(event.module)} · ${
                                    Instant.ofEpochMilli(event.practicedAt).atZone(zone).format(calendarTimeFormatter)
                                }",
                                color = moduleColor(event.module),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text("查看", color = moduleColor(event.module), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = date != null,
        modifier = modifier.aspectRatio(1f).padding(2.dp),
        shape = RoundedCornerShape(13.dp),
        color = when {
            selected -> LocalPrepColors.current.primary
            count > 0 -> LocalPrepColors.current.primary.copy(alpha = .12f)
            else -> Color.Transparent
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                date?.dayOfMonth?.toString().orEmpty(),
                color = if (selected) Color.White else Color(0xFF33343A),
                fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
            )
            if (count > 0) {
                Spacer(Modifier.padding(top = 1.dp))
                Text(
                    "$count 次",
                    color = if (selected) Color.White else LocalPrepColors.current.primary,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

private fun moduleLabel(module: PracticeModule): String = when (module) {
    PracticeModule.TRIAL -> "试讲"
    PracticeModule.STRUCTURED -> "结构化"
    PracticeModule.TEMPLATE -> "模板"
}

private fun moduleColor(module: PracticeModule): Color = when (module) {
    PracticeModule.TRIAL -> Color(0xFF2888F5)
    PracticeModule.STRUCTURED -> Color(0xFF7654F6)
    PracticeModule.TEMPLATE -> Color(0xFFFF7C20)
}
