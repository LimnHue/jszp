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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AnswerTemplate
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.ScopeConfig
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
fun StructuredScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onSwitchScope: () -> Unit,
    onImport: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onEdit: (String) -> Unit,
    onPractice: (String) -> Unit,
    initialSelectedId: String? = null,
    drawMode: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val scope = data.preferences.selectedScope
    val filterVisibility = data.preferences.filterVisibility
    val config = data.scopeConfigs[scope.key] ?: com.shangan.teacherprep.data.ScopeDefaults.create(scope)
    var category by remember { mutableStateOf("全部") }
    var importance by remember { mutableStateOf("全部") }
    var selectedId by remember(initialSelectedId) { mutableStateOf(initialSelectedId) }
    val filtered = data.structuredQuestions.filter {
        it.scopeKey == scope.key &&
            (
                drawMode ||
                    (
                        (!filterVisibility.structuredCategory || category == "全部" || it.category == category) &&
                            (!filterVisibility.structuredImportance || importance == "全部" || it.importance == importance.substringBefore("星").trim().toIntOrNull())
                    )
                )
    }
    val selected = filtered.firstOrNull { it.id == selectedId } ?: filtered.firstOrNull()

    Scaffold(containerColor = Color(0xFFF8F6FF)) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + inner.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + inner.calculateBottomPadding() + 82.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                if (drawMode) {
                    ScreenHeader(title = "结构化抽题", onBack = onBack)
                } else {
                    ScreenHeader("结构化", scope, onSwitchScope)
                }
            }
            if (!drawMode) item {
                ModuleImportEntry(
                    text = "导入结构化问题",
                    onClick = onImport,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color(0xFF7654F6),
                )
            }
            if (!drawMode && (filterVisibility.structuredCategory || filterVisibility.structuredImportance)) item {
                SectionPanel(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    color = Color(0xFF7654F6),
                ) {
                    Text(
                        "题目筛选",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFF6046D9),
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                    )
                    if (filterVisibility.structuredCategory) {
                        PracticeFilterLabel("问题种类", Color(0xFF6046D9))
                        FilterChips(
                            config.structuredTypes,
                            category,
                            { category = it; selectedId = null },
                            horizontalPadding = 16,
                        )
                    }
                    if (filterVisibility.structuredImportance) {
                        PracticeFilterLabel("重要程度", Color(0xFF6046D9))
                        FilterChips(
                            listOf("5 星", "4 星", "3 星", "2 星", "1 星"),
                            importance,
                            { importance = it; selectedId = null },
                            horizontalPadding = 16,
                        )
                    }
                }
            }
            if (selected != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = Color.Transparent,
                        shadowElevation = 7.dp,
                    ) {
                        Column(
                            Modifier.background(Brush.linearGradient(listOf(Color(0xFF5D42E8), Color(0xFFFF7081))))
                                .padding(22.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .18f)) {
                                    Text(selected.category, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { onToggleFavorite(selected.id) }) {
                                    Icon(if (selected.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "收藏", tint = Color.White)
                                }
                                if (!drawMode) {
                                    IconButton(onClick = { onEdit(selected.id) }) {
                                        Icon(Icons.Rounded.Edit, "修改结构化问题", tint = Color.White)
                                    }
                                }
                            }
                            Text(selected.question, color = Color.White, fontWeight = FontWeight.Black, fontSize = 27.sp, lineHeight = 36.sp)
                            ImportanceStars(
                                selected.importance,
                                modifier = Modifier.padding(top = 8.dp),
                                activeColor = Color(0xFFFFD15C),
                                inactiveColor = Color.White.copy(alpha = .5f),
                                iconSize = 22,
                            )
                            Text(
                                practiceHistoryText(selected.practiceCount, selected.lastPracticedAt),
                                modifier = Modifier.padding(top = 8.dp),
                                color = Color.White.copy(alpha = .9f),
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(18.dp))
                            Surface(
                                onClick = { selectedId = filtered.filterNot { it.id == selected.id }.randomOrNull()?.id ?: selected.id },
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Row(Modifier.padding(horizontal = 28.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Shuffle, null, tint = Color(0xFF6649EE))
                                    Text("  换一题", color = Color(0xFF6649EE), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item {
                    RoundedCard(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        containerColor = Color(0xFFFCFAFF),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFF7654F6))
                            Text("  答题思路", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF6046D9))
                        }
                        selected.answerSections.forEachIndexed { index, section ->
                            Surface(
                                color = Color(0xFFF9F7FF),
                                shape = RoundedCornerShape(17.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            ) {
                                Column(Modifier.padding(15.dp)) {
                                    Text("${index + 1}  ${section.title}", color = Color(0xFF6046D9), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    MarkdownText(section.markdown, Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                }
                item {
                    PracticeTimer(
                        data.preferences,
                        selected.durationMinutes,
                        Modifier.padding(horizontal = 20.dp),
                        onPracticeStarted = { onPractice(selected.id) },
                    )
                }
            } else {
                item { EmptyPracticeState("当前题库还没有结构化问题", onImport) }
            }
            if (!drawMode && filtered.size > 1) {
                item { Text("题目列表", Modifier.padding(horizontal = 20.dp), fontSize = 21.sp, fontWeight = FontWeight.Black) }
                items(filtered) { item ->
                    RoundedCard(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        onClick = { selectedId = item.id },
                        containerColor = Color(0xFFFCFAFF),
                    ) {
                        Text(item.category, color = Color(0xFF7654F6), fontWeight = FontWeight.Bold)
                        Text(item.question, modifier = Modifier.padding(top = 6.dp), fontWeight = FontWeight.Bold)
                        ImportanceStars(item.importance, modifier = Modifier.padding(top = 6.dp), iconSize = 18)
                        Text(
                            practiceHistoryText(item.practiceCount, item.lastPracticedAt),
                            modifier = Modifier.padding(top = 7.dp),
                            color = Color.Gray,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onSwitchScope: () -> Unit,
    onImport: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onEdit: (String) -> Unit,
    onPractice: (String) -> Unit,
    initialExpandedId: String? = null,
    focusMode: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val scope = data.preferences.selectedScope
    val filterVisibility = data.preferences.filterVisibility
    val config = data.scopeConfigs[scope.key] ?: com.shangan.teacherprep.data.ScopeDefaults.create(scope)
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }
    var expanded by remember(initialExpandedId) { mutableStateOf(initialExpandedId) }
    val filtered = data.templates.filter {
        it.scopeKey == scope.key &&
            (!focusMode || it.id == initialExpandedId) &&
            (!filterVisibility.templateCategory || category == "全部" || it.category == category) &&
            (!filterVisibility.templateSearch || query.isBlank() || it.name.contains(query, true) || it.contentMarkdown.contains(query, true))
    }

    Scaffold(containerColor = Color(0xFFFFF9F3)) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + inner.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + inner.calculateBottomPadding() + 82.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                if (focusMode) ScreenHeader("模板练习", onBack = onBack)
                else ScreenHeader("模板", scope, onSwitchScope)
            }
            if (!focusMode) item {
                ModuleImportEntry(
                    text = "导入答题模板",
                    onClick = onImport,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color(0xFFFF7C20),
                )
            }
            if (!focusMode && (filterVisibility.templateSearch || filterVisibility.templateCategory)) item {
                SectionPanel(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    color = Color(0xFFFF7C20),
                ) {
                    Text(
                        "模板筛选",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFFE9650B),
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                    )
                    if (filterVisibility.templateSearch) {
                        OutlinedTextField(
                            query,
                            { query = it },
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            placeholder = { Text("搜索模板名称或关键词") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                        )
                    }
                    if (filterVisibility.templateCategory) {
                        PracticeFilterLabel("模板种类", Color(0xFFE9650B))
                        FilterChips(config.templateTypes, category, { category = it }, horizontalPadding = 16)
                    }
                }
            }
            if (!focusMode) item {
                Surface(
                    onClick = {
                        filtered.randomOrNull()?.let {
                            expanded = it.id
                            onPractice(it.id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFFF3E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC89D)),
                ) {
                    Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shuffle, null, tint = Color(0xFFFF6F1A), modifier = Modifier.size(45.dp))
                        Column(Modifier.padding(start = 18.dp).weight(1f)) {
                            Text("随机模板", fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text("抽一个框架，限时表达", color = Color.Gray)
                        }
                        Text("开始抽取", color = Color(0xFFFF6F1A), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyPracticeState("当前题库还没有模板", onImport) }
            } else {
                items(filtered) { item ->
                    TemplateCard(
                        item = item,
                        expanded = expanded == item.id,
                        onClick = {
                            if (expanded == item.id) {
                                expanded = null
                            } else {
                                expanded = item.id
                                onPractice(item.id)
                            }
                        },
                        onFavorite = { onToggleFavorite(item.id) },
                        onEdit = { onEdit(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    item: AnswerTemplate,
    expanded: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
) {
    RoundedCard(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        onClick = onClick,
        containerColor = Color(0xFFFFFDF9),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF0E7)) {
                Text(item.category, color = Color(0xFFFF6F1A), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFavorite) {
                Icon(if (item.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "收藏", tint = Color(0xFFFF6F1A))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, "修改模板", tint = Color(0xFFFF6F1A))
            }
        }
        Text(item.name, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(item.summary, color = Color.Gray, modifier = Modifier.padding(top = 5.dp))
        Text(
            practiceHistoryText(item.practiceCount, item.lastPracticedAt),
            color = Color(0xFFFF6F1A),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 7.dp),
        )
        if (expanded) {
            Surface(color = Color(0xFFFFFAF6), shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 14.dp)) {
                MarkdownText(item.contentMarkdown, Modifier.padding(14.dp))
            }
        }
    }
}

@Composable
private fun PracticeFilterLabel(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
private fun EmptyPracticeState(text: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Color.Gray)
        Surface(onClick = onAction, color = LocalPrepColors.current.primary, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 12.dp)) {
            Text("立即导入", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        }
    }
}
