package com.shangan.teacherprep.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.AppPreferences
import com.shangan.teacherprep.data.PracticeMediaType
import com.shangan.teacherprep.data.TrialLesson
import com.shangan.teacherprep.ui.FilterChips
import com.shangan.teacherprep.ui.ImportanceStars
import com.shangan.teacherprep.ui.MarkdownText
import com.shangan.teacherprep.ui.ModuleImportEntry
import com.shangan.teacherprep.ui.PracticeTimer
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScreenHeader
import com.shangan.teacherprep.ui.SectionPanel
import com.shangan.teacherprep.ui.practiceHistoryText
import com.shangan.teacherprep.ui.theme.LocalPrepColors

@Composable
fun TrialLibraryScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onSwitchScope: () -> Unit,
    onOpen: (String) -> Unit,
    onImport: () -> Unit,
) {
    val scope = data.preferences.selectedScope
    val filterVisibility = data.preferences.filterVisibility
    val config = data.scopeConfigs[scope.key] ?: com.shangan.teacherprep.data.ScopeDefaults.create(scope)
    var query by remember { mutableStateOf("") }
    var textbook by remember { mutableStateOf("全部") }
    var unit by remember { mutableStateOf("全部") }
    var genre by remember { mutableStateOf("全部") }
    var importance by remember { mutableStateOf("全部") }
    val items = data.trials
        .filter {
            it.scopeKey == scope.key &&
                (!filterVisibility.trialSearch || query.isBlank() || it.title.contains(query, true) || it.courseInfoMarkdown.contains(query, true)) &&
                (!filterVisibility.trialTextbook || textbook == "全部" || it.textbook == textbook) &&
                (!filterVisibility.trialUnit || unit == "全部" || it.unit == unit) &&
                (!filterVisibility.trialGenre || genre == "全部" || it.genre == genre) &&
                (!filterVisibility.trialImportance || importance == "全部" || it.importance == importance.substringBefore("星").trim().toIntOrNull())
        }
        .sortedWith(
            compareBy<TrialLesson>(
                { config.textbooks.indexOf(it.textbook).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE },
                { config.units.indexOf(it.unit).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE },
                { it.lessonOrder.takeIf { order -> order > 0 } ?: Int.MAX_VALUE },
                { it.title },
            ),
        )

    Scaffold(
        containerColor = LocalPrepColors.current.primary.copy(alpha = .035f),
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + inner.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + inner.calculateBottomPadding() + 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenHeader("试讲", scope, onSwitchScope) }
            item {
                ModuleImportEntry(
                    text = "导入试讲课程",
                    onClick = onImport,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item {
                SectionPanel(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    color = LocalPrepColors.current.primary,
                ) {
                    Text(
                        "课程筛选",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = LocalPrepColors.current.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                    )
                    if (filterVisibility.trialSearch) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            placeholder = { Text("搜索课程、课文名") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                        )
                    }
                    if (filterVisibility.trialTextbook) {
                        FilterLabel("教材")
                        FilterChips(config.textbooks, textbook, { textbook = it }, horizontalPadding = 16)
                    }
                    if (filterVisibility.trialUnit) {
                        FilterLabel("单元")
                        FilterChips(config.units, unit, { unit = it }, horizontalPadding = 16)
                    }
                    if (filterVisibility.trialGenre) {
                        FilterLabel("题材")
                        FilterChips(config.genres, genre, { genre = it }, horizontalPadding = 16)
                    }
                    if (filterVisibility.trialImportance) {
                        FilterLabel("重要程度")
                        FilterChips(
                            listOf("5 星", "4 星", "3 星", "2 星", "1 星"),
                            importance,
                            { importance = it },
                            horizontalPadding = 16,
                        )
                    }
                }
            }
            item {
                val random = items.randomOrNull()
                Surface(
                    onClick = { random?.let { onOpen(it.id) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                ) {
                    Row(
                        Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFF806B), LocalPrepColors.current.primary)))
                            .padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = Color.White) {
                            Icon(Icons.Rounded.Shuffle, null, tint = LocalPrepColors.current.primary, modifier = Modifier.padding(15.dp).size(30.dp))
                        }
                        Column(Modifier.padding(start = 18.dp)) {
                            Text("随机抽课", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text("从当前筛选中抽取一道", color = Color.White.copy(alpha = .85f))
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item { EmptyState("当前筛选下没有试讲课程", onImport) }
            } else {
                item {
                    Text(
                        "课程列表 · ${items.size} 条",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                    )
                }
                items(items.size) { index -> TrialCard(items[index], index, onOpen) }
            }
        }
    }
}

@Composable
private fun TrialCard(item: TrialLesson, index: Int, onOpen: (String) -> Unit) {
    RoundedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        onClick = { onOpen(item.id) },
        containerColor = if (index % 2 == 0) Color.White else Color(0xFFF1F6FF),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(92.dp).background(LocalPrepColors.current.primary.copy(alpha = .1f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Image, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(36.dp))
            }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(lessonDisplayTitle(item), fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(listOf(item.textbook, item.unit, item.genre).filter { it.isNotBlank() }.joinToString(" · "), color = Color.Gray)
                ImportanceStars(item.importance, modifier = Modifier.padding(top = 5.dp), iconSize = 18)
                Spacer(Modifier.height(9.dp))
                Text("${item.durationMinutes} 分钟", fontSize = 13.sp, color = Color.Gray)
                Text(
                    practiceHistoryText(item.practiceCount, item.lastPracticedAt),
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    color = LocalPrepColors.current.primary,
                )
            }
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color(0xFF44454D),
    )
}

@Composable
private fun EmptyState(message: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Add, null, tint = Color.Gray, modifier = Modifier.size(50.dp))
        Text(message, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
        Surface(onClick = onAction, color = LocalPrepColors.current.primary, shape = RoundedCornerShape(50)) {
            Text("导入内容", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        }
    }
}

@Composable
fun TrialDetailScreen(
    lesson: TrialLesson,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onPractice: () -> Unit,
    onMediaSaved: (PracticeMediaType, String) -> Unit,
) {
    var tab by remember { mutableStateOf(1) }
    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenHeader(
                title = lesson.title,
                onBack = onBack,
                action = {
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "修改课程",
                                tint = LocalPrepColors.current.primary,
                            )
                        }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (lesson.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = LocalPrepColors.current.primary,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            PracticeTimer(
                preferences = preferences,
                minutes = lesson.durationMinutes,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                onPracticeStarted = onPractice,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(lesson.author, fontSize = 18.sp, color = Color(0xFF55565D))
                Text(
                    listOf(
                        lesson.textbook,
                        lesson.unit,
                        lesson.lessonOrder.takeIf { it > 0 }?.let { "第${it}课" }.orEmpty(),
                        lesson.genre,
                    ).filter { it.isNotBlank() }.joinToString("  |  "),
                    color = LocalPrepColors.current.primary,
                )
                ImportanceStars(lesson.importance, modifier = Modifier.padding(top = 6.dp), iconSize = 20)
                Text(
                    practiceHistoryText(lesson.practiceCount, lesson.lastPracticedAt),
                    modifier = Modifier.padding(top = 6.dp),
                    color = Color.Gray,
                    fontSize = 13.sp,
                )
            }
            val labels = listOf("课程信息", "试讲流程", "板书设计", "练习记录")
            TabRow(selectedTabIndex = tab, containerColor = LocalPrepColors.current.primary.copy(alpha = .055f)) {
                labels.forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label, fontWeight = FontWeight.Bold) })
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(LocalPrepColors.current.primary.copy(alpha = .035f)),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tab) {
                    0 -> item {
                        RoundedCard(containerColor = Color(0xFFF4F8FF)) {
                            Text("课时信息", fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (lesson.lessonOrder > 0) "第${lesson.lessonOrder}课 · ${lesson.textbook} · ${lesson.unit}" else "${lesson.textbook} · ${lesson.unit}",
                                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                                color = LocalPrepColors.current.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            MarkdownText(lesson.courseInfoMarkdown)
                        }
                    }
                    1 -> items(lesson.bodySections.size) { index ->
                        val section = lesson.bodySections[index]
                        RoundedCard(
                            containerColor = if (index % 2 == 0) Color(0xFFF7FAFF) else Color(0xFFFFF7F5),
                        ) {
                            Text("${index + 1}、${section.title}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF202126))
                            Spacer(Modifier.height(12.dp))
                            MarkdownText(section.markdown)
                        }
                    }
                    2 -> item {
                        RoundedCard(containerColor = Color(0xFFF4FAF7)) {
                            Text("板书设计图", fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(14.dp))
                            if (lesson.boardImageUri != null) {
                                AsyncImage(
                                    model = lesson.boardImageUri,
                                    contentDescription = "板书设计图",
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1.15f),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                BlackboardPlaceholder(lesson.title)
                            }
                        }
                    }
                    3 -> item {
                        TrialMediaSection(lesson = lesson, onMediaSaved = onMediaSaved)
                    }
                }
            }
        }
    }
}

private fun lessonDisplayTitle(lesson: TrialLesson): String {
    return if (lesson.lessonOrder > 0) "第${lesson.lessonOrder}课 · ${lesson.title}" else lesson.title
}

@Composable
private fun BlackboardPlaceholder(title: String) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.15f)
            .background(Color(0xFF173E34), RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width * .5f, size.height * .45f)
                lineTo(size.width * .22f, size.height * .25f)
                moveTo(size.width * .5f, size.height * .45f)
                lineTo(size.width * .78f, size.height * .25f)
                moveTo(size.width * .5f, size.height * .5f)
                lineTo(size.width * .5f, size.height * .75f)
            }
            drawPath(path, Color.White.copy(alpha = .65f), style = Stroke(width = 4f))
        }
        Text(title.trim('《', '》'), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Light)
    }
}
