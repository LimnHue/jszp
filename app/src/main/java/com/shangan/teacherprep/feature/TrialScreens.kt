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
import androidx.compose.material3.LinearProgressIndicator
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
import com.shangan.teacherprep.data.TrialLesson
import com.shangan.teacherprep.ui.FilterChips
import com.shangan.teacherprep.ui.MarkdownText
import com.shangan.teacherprep.ui.ModuleImportEntry
import com.shangan.teacherprep.ui.PracticeTimer
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScreenHeader
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
    val config = data.scopeConfigs[scope.key] ?: com.shangan.teacherprep.data.ScopeConfig()
    var query by remember { mutableStateOf("") }
    var textbook by remember { mutableStateOf("全部") }
    var genre by remember { mutableStateOf("全部") }
    val items = data.trials.filter {
        it.scopeKey == scope.key &&
            (query.isBlank() || it.title.contains(query, true) || it.courseInfoMarkdown.contains(query, true)) &&
            (textbook == "全部" || it.textbook == textbook) &&
            (genre == "全部" || it.genre == genre)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    placeholder = { Text("搜索课程、课文名") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                )
            }
            item { Text("教材", modifier = Modifier.padding(horizontal = 20.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            item { FilterChips(config.textbooks, textbook, { textbook = it }) }
            item { Text("题材", modifier = Modifier.padding(horizontal = 20.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            item { FilterChips(config.genres, genre, { genre = it }) }
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
                items(items.size) { index -> TrialCard(items[index], onOpen) }
            }
        }
    }
}

@Composable
private fun TrialCard(item: TrialLesson, onOpen: (String) -> Unit) {
    RoundedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        onClick = { onOpen(item.id) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(92.dp).background(LocalPrepColors.current.primary.copy(alpha = .1f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Image, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(36.dp))
            }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(item.title, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("${item.textbook} · ${item.genre}", color = Color.Gray)
                Spacer(Modifier.height(9.dp))
                Row {
                    Text("${item.durationMinutes} 分钟", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    Text("${(item.progress * 100).toInt()}%", color = LocalPrepColors.current.primary)
                }
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    color = LocalPrepColors.current.primary,
                    trackColor = Color(0xFFE9E9EA),
                )
            }
        }
    }
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
                mode = preferences.timerMode,
                minutes = lesson.durationMinutes,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(lesson.author, fontSize = 18.sp, color = Color(0xFF55565D))
                Text("${lesson.textbook}  |  ${lesson.genre}", color = LocalPrepColors.current.primary)
            }
            val labels = listOf("课程信息", "试讲主体", "板书设计")
            TabRow(selectedTabIndex = tab, containerColor = Color.White) {
                labels.forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label, fontWeight = FontWeight.Bold) })
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tab) {
                    0 -> item {
                        RoundedCard { MarkdownText(lesson.courseInfoMarkdown) }
                    }
                    1 -> items(lesson.bodySections.size) { index ->
                        val section = lesson.bodySections[index]
                        RoundedCard {
                            Text("${index + 1}、${section.title}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF202126))
                            Spacer(Modifier.height(12.dp))
                            MarkdownText(section.markdown)
                        }
                    }
                    2 -> item {
                        RoundedCard {
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
                }
            }
        }
    }
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
