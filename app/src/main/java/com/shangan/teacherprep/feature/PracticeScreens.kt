package com.shangan.teacherprep.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.shangan.teacherprep.data.AppPreferences
import com.shangan.teacherprep.data.PracticeMediaType
import com.shangan.teacherprep.data.PracticeModule
import com.shangan.teacherprep.data.SharedLibrary
import com.shangan.teacherprep.data.StructuredQuestion
import com.shangan.teacherprep.ui.BatchActionBar
import com.shangan.teacherprep.ui.FilterChips
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.ui.EditableSwipeCard
import com.shangan.teacherprep.ui.ImportanceStars
import com.shangan.teacherprep.ui.MarkdownText
import com.shangan.teacherprep.ui.ModuleImportEntry
import com.shangan.teacherprep.ui.PracticeTimer
import com.shangan.teacherprep.ui.RandomDrawCandidate
import com.shangan.teacherprep.ui.RandomDrawDialog
import com.shangan.teacherprep.ui.RandomDrawGroup
import com.shangan.teacherprep.ui.randomDrawId
import com.shangan.teacherprep.ui.savedRandomDrawSelections
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
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteBatch: (Set<String>) -> Unit,
    onExportBatch: (Set<String>) -> Unit,
    onUpdateDrawSelections: (PracticeModule, Map<String, Set<String>>) -> Unit,
) {
    val scope = data.preferences.selectedScope
    val visibility = data.preferences.filterVisibility
    val config = data.scopeConfigs[scope.key] ?: com.shangan.teacherprep.data.ScopeDefaults.create(scope)
    var category by remember { mutableStateOf("全部") }
    var importance by remember { mutableStateOf("全部") }
    var showDrawDialog by remember { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val scopeItems = data.structuredQuestions.filter { it.scopeKey == SharedLibrary.key }
    val drawGroups = listOf(
        RandomDrawGroup("category", "问题种类", scopeItems.map { it.category }.distinct()),
        RandomDrawGroup("importance", "重要程度", scopeItems.map { "${it.importance} 星" }.distinct().sortedDescending()),
    )
    val drawCandidates = scopeItems.map {
        RandomDrawCandidate(
            it.id,
            mapOf("category" to it.category, "importance" to "${it.importance} 星"),
        )
    }
    val drawSelections = savedRandomDrawSelections(data.preferences, SharedLibrary.key, PracticeModule.STRUCTURED)
    val filtered = scopeItems.filter {
        (!visibility.structuredCategory || category == "全部" || it.category == category) &&
            (!visibility.structuredImportance || importance == "全部" || it.importance == importance.substringBefore("星").trim().toIntOrNull())
    }
    val listState = rememberLazyListState()
    val visibleIds = remember(filtered) { filtered.map { it.id }.toSet() }

    LaunchedEffect(visibleIds) {
        selectedIds = selectedIds.intersect(visibleIds)
        if (visibleIds.isEmpty()) batchMode = false
    }

    Scaffold(containerColor = Color(0xFFF7F5F1)) { inner ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + inner.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + inner.calculateBottomPadding() + 82.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            item { ScreenHeader("结构化", scope, onSwitchScope) }
            item {
                ModuleImportEntry(
                    text = "导入结构化问题",
                    onClick = onImport,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = LocalPrepColors.current.primary,
                )
            }
            if (visibility.structuredCategory || visibility.structuredImportance) {
                item {
                    SectionPanel(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        color = LocalPrepColors.current.primary,
                    ) {
                        Text("题目筛选", Modifier.padding(horizontal = 16.dp), color = LocalPrepColors.current.primary, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        if (visibility.structuredCategory) {
                            PracticeFilterLabel("问题种类", LocalPrepColors.current.primary)
                            FilterChips(config.structuredTypes, category, { category = it }, horizontalPadding = 16)
                        }
                        if (visibility.structuredImportance) {
                            PracticeFilterLabel("重要程度", LocalPrepColors.current.primary)
                            FilterChips(listOf("5 星", "4 星", "3 星", "2 星", "1 星"), importance, { importance = it }, horizontalPadding = 16)
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = { randomDrawId(drawGroups, drawCandidates, drawSelections)?.let(onOpen) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFFCFBF8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
                    ) {
                        Row(
                            Modifier.padding(17.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Shuffle, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(32.dp))
                            Column(Modifier.padding(start = 18.dp)) {
                                Text("随机抽题", color = Color(0xFF202224), fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Text("点击立即从已设范围抽取", color = Color(0xFF74777A), fontSize = 13.sp)
                            }
                        }
                    }
                    Surface(
                        onClick = { showDrawDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFE9EEF2),
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "设置抽取范围",
                            tint = LocalPrepColors.current.primary,
                            modifier = Modifier.padding(17.dp).size(25.dp),
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyPracticeState("当前题库还没有结构化问题", onImport) }
            } else {
                item {
                    Text("题目列表 · ${filtered.size} 条", Modifier.padding(horizontal = 20.dp), fontSize = 21.sp, fontWeight = FontWeight.Black)
                }
                if (batchMode) {
                    item {
                        BatchActionBar(
                            selectedCount = selectedIds.size,
                            totalCount = filtered.size,
                            itemLabel = "结构化",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            onSelectAll = { selectedIds = visibleIds },
                            onClear = {
                                batchMode = false
                                selectedIds = emptySet()
                            },
                            onDelete = {
                                onDeleteBatch(selectedIds)
                                batchMode = false
                                selectedIds = emptySet()
                            },
                            onExport = { onExportBatch(selectedIds) },
                        )
                    }
                }
                items(filtered, key = { it.id }) { item ->
                    EditableSwipeCard(
                        itemLabel = item.question,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        selectionMode = batchMode,
                        selected = item.id in selectedIds,
                        onSelectionChange = { selected ->
                            batchMode = true
                            selectedIds = if (selected) selectedIds + item.id else selectedIds - item.id
                        },
                        onOpen = { onOpen(item.id) },
                        onEdit = { onEdit(item.id) },
                        onDelete = { onDelete(item.id) },
                        containerColor = Color.White,
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFE9EEF2)) {
                            Text(item.category, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(item.question, modifier = Modifier.padding(top = 9.dp), fontWeight = FontWeight.Black, fontSize = 17.sp)
                        ImportanceStars(item.importance, modifier = Modifier.padding(top = 7.dp), iconSize = 15)
                        Text(practiceHistoryText(item.practiceCount, item.lastPracticedAt), Modifier.padding(top = 6.dp), color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
            }
            DraggableScrollToTopButton(
                listState,
                Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }

    if (showDrawDialog) {
        RandomDrawDialog(
            title = "按标签抽结构化",
            groups = drawGroups,
            candidates = drawCandidates,
            initialSelections = drawSelections,
            onDismiss = { showDrawDialog = false },
            onSave = {
                onUpdateDrawSelections(PracticeModule.STRUCTURED, it)
                showDrawDialog = false
            },
            accent = LocalPrepColors.current.primary,
        )
    }
}

@Composable
fun StructuredDetailScreen(
    question: StructuredQuestion,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onPractice: () -> Unit,
    onMediaSaved: (PracticeMediaType, String) -> Unit,
    onMediaDelete: (String) -> Unit,
    onMediaRename: (String, String) -> Unit,
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFFF7F5F1),
        topBar = {
            ScreenHeader(
                "结构化练习",
                onBack = onBack,
                action = {
                    Row {
                        IconButton(onClick = onExport) { Icon(Icons.Rounded.Share, "导出问题", tint = LocalPrepColors.current.primary) }
                        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "修改问题", tint = LocalPrepColors.current.primary) }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(if (question.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "收藏", tint = LocalPrepColors.current.primary)
                        }
                    }
                },
            )
        },
        bottomBar = {
            PracticeTimer(preferences, question.durationMinutes, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), onPracticeStarted = onPractice)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = LocalPrepColors.current.primary) {
                    Column(Modifier.padding(20.dp)) {
                        Text(question.category, color = Color.White.copy(alpha = .9f), fontWeight = FontWeight.Bold)
                        Text(question.question, color = Color.White, fontWeight = FontWeight.Black, fontSize = 27.sp, lineHeight = 36.sp, modifier = Modifier.padding(top = 10.dp))
                        ImportanceStars(question.importance, Modifier.padding(top = 10.dp), activeColor = Color(0xFFFFD15C), inactiveColor = Color.White.copy(alpha = .5f), iconSize = 22)
                        Text(practiceHistoryText(question.practiceCount, question.lastPracticedAt), Modifier.padding(top = 8.dp), color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
                    }
                }
            }
            item {
                RoundedCard(containerColor = Color(0xFFFCFBF8)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = LocalPrepColors.current.primary)
                        Text("  答题思路", fontSize = 20.sp, fontWeight = FontWeight.Black, color = LocalPrepColors.current.primary)
                    }
                    question.answerSections.forEachIndexed { index, section ->
                        Surface(color = Color(0xFFE9EEF2).copy(alpha = .55f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                Text("${index + 1}  ${section.title}", color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                MarkdownText(section.markdown, Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
            item {
                PracticeMediaSection(
                    itemId = question.id,
                    mediaItems = question.practiceMedia,
                    practiceLabel = "结构化",
                    onMediaSaved = onMediaSaved,
                    onMediaDelete = onMediaDelete,
                    onMediaRename = onMediaRename,
                )
            }
            }
            DraggableScrollToTopButton(
                listState,
                Modifier.padding(bottom = padding.calculateBottomPadding()),
            )
        }
    }
}

@Composable
fun TemplateScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onSwitchScope: () -> Unit,
    onImport: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteBatch: (Set<String>) -> Unit,
    onExportBatch: (Set<String>) -> Unit,
    onUpdateDrawSelections: (PracticeModule, Map<String, Set<String>>) -> Unit,
) {
    val scope = data.preferences.selectedScope
    val visibility = data.preferences.filterVisibility
    val config = data.scopeConfigs[scope.key] ?: com.shangan.teacherprep.data.ScopeDefaults.create(scope)
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }
    var showDrawDialog by remember { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val scopeItems = data.templates.filter { it.scopeKey == SharedLibrary.key }
    val drawGroups = listOf(
        RandomDrawGroup("category", "模板种类", scopeItems.map { it.category }.distinct()),
        RandomDrawGroup("module", "适用模块", scopeItems.map { it.module }.filter { it.isNotBlank() }.distinct()),
    )
    val drawCandidates = scopeItems.map {
        RandomDrawCandidate(
            it.id,
            mapOf("category" to it.category, "module" to it.module),
        )
    }
    val drawSelections = savedRandomDrawSelections(data.preferences, SharedLibrary.key, PracticeModule.TEMPLATE)
    val filtered = scopeItems.filter {
        (!visibility.templateCategory || category == "全部" || it.category == category) &&
            (!visibility.templateSearch || query.isBlank() || it.name.contains(query, true) || it.contentMarkdown.contains(query, true))
    }
    val listState = rememberLazyListState()
    val visibleIds = remember(filtered) { filtered.map { it.id }.toSet() }

    LaunchedEffect(visibleIds) {
        selectedIds = selectedIds.intersect(visibleIds)
        if (visibleIds.isEmpty()) batchMode = false
    }

    Scaffold(containerColor = Color(0xFFF7F5F1)) { inner ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + inner.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + inner.calculateBottomPadding() + 82.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            item { ScreenHeader("模板", scope, onSwitchScope) }
            item {
                ModuleImportEntry("导入答题模板", onImport, Modifier.padding(horizontal = 20.dp), LocalPrepColors.current.primary)
            }
            if (visibility.templateSearch || visibility.templateCategory) {
                item {
                    SectionPanel(Modifier.fillMaxWidth().padding(horizontal = 20.dp), LocalPrepColors.current.primary) {
                        Text("模板筛选", Modifier.padding(horizontal = 16.dp), color = LocalPrepColors.current.primary, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        if (visibility.templateSearch) {
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
                        if (visibility.templateCategory) {
                            PracticeFilterLabel("模板种类", LocalPrepColors.current.primary)
                            FilterChips(config.templateTypes, category, { category = it }, horizontalPadding = 16)
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = { randomDrawId(drawGroups, drawCandidates, drawSelections)?.let(onOpen) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFFCFBF8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
                    ) {
                        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shuffle, null, tint = LocalPrepColors.current.primary, modifier = Modifier.size(34.dp))
                            Column(Modifier.padding(start = 18.dp).weight(1f)) {
                                Text("随机模板", fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Text("点击立即从已设范围抽取", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                    Surface(
                        onClick = { showDrawDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFE9EEF2),
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "设置抽取范围",
                            tint = LocalPrepColors.current.primary,
                            modifier = Modifier.padding(17.dp).size(25.dp),
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyPracticeState("当前题库还没有模板", onImport) }
            } else {
                if (batchMode) {
                    item {
                        BatchActionBar(
                            selectedCount = selectedIds.size,
                            totalCount = filtered.size,
                            itemLabel = "模板",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            onSelectAll = { selectedIds = visibleIds },
                            onClear = {
                                batchMode = false
                                selectedIds = emptySet()
                            },
                            onDelete = {
                                onDeleteBatch(selectedIds)
                                batchMode = false
                                selectedIds = emptySet()
                            },
                            onExport = { onExportBatch(selectedIds) },
                        )
                    }
                }
                items(filtered, key = { it.id }) { item ->
                    TemplateCard(
                        item = item,
                        selectionMode = batchMode,
                        selected = item.id in selectedIds,
                        onSelectionChange = { selected ->
                            batchMode = true
                            selectedIds = if (selected) selectedIds + item.id else selectedIds - item.id
                        },
                        onClick = { onOpen(item.id) },
                        onEdit = { onEdit(item.id) },
                        onDelete = { onDelete(item.id) },
                    )
                }
            }
            }
            DraggableScrollToTopButton(
                listState,
                Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }

    if (showDrawDialog) {
        RandomDrawDialog(
            title = "按标签抽模板",
            groups = drawGroups,
            candidates = drawCandidates,
            initialSelections = drawSelections,
            onDismiss = { showDrawDialog = false },
            onSave = {
                onUpdateDrawSelections(PracticeModule.TEMPLATE, it)
                showDrawDialog = false
            },
            accent = LocalPrepColors.current.primary,
        )
    }
}

@Composable
fun TemplateDetailScreen(
    template: AnswerTemplate,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onPractice: () -> Unit,
    onMediaSaved: (PracticeMediaType, String) -> Unit,
    onMediaDelete: (String) -> Unit,
    onMediaRename: (String, String) -> Unit,
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFFF7F5F1),
        topBar = {
            ScreenHeader(
                "模板练习",
                onBack = onBack,
                action = {
                    Row {
                        IconButton(onClick = onExport) { Icon(Icons.Rounded.Share, "导出模板", tint = LocalPrepColors.current.primary) }
                        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "修改模板", tint = LocalPrepColors.current.primary) }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(if (template.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "收藏", tint = LocalPrepColors.current.primary)
                        }
                    }
                },
            )
        },
        bottomBar = {
            PracticeTimer(preferences, preferences.defaultStructuredMinutes, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), onPracticeStarted = onPractice)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            item {
                RoundedCard(containerColor = Color(0xFFFCFBF8)) {
                    Text(template.category, color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold)
                    Text(template.name, fontSize = 25.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
                    Text(practiceHistoryText(template.practiceCount, template.lastPracticedAt), color = LocalPrepColors.current.primary, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
                    Surface(color = Color(0xFFE9EEF2).copy(alpha = .45f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        MarkdownText(template.contentMarkdown, Modifier.padding(14.dp))
                    }
                }
            }
            item {
                PracticeMediaSection(
                    itemId = template.id,
                    mediaItems = template.practiceMedia,
                    practiceLabel = "模板",
                    onMediaSaved = onMediaSaved,
                    onMediaDelete = onMediaDelete,
                    onMediaRename = onMediaRename,
                )
            }
            }
            DraggableScrollToTopButton(
                listState,
                Modifier.padding(bottom = padding.calculateBottomPadding()),
            )
        }
    }
}

@Composable
private fun TemplateCard(
    item: AnswerTemplate,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    EditableSwipeCard(
        itemLabel = item.name,
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        selectionMode = selectionMode,
        selected = selected,
        onSelectionChange = onSelectionChange,
        onOpen = onClick,
        onEdit = onEdit,
        onDelete = onDelete,
        containerColor = Color.White,
    ) {
        Surface(shape = RoundedCornerShape(50), color = Color(0xFFE9EEF2)) {
            Text(item.category, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(item.name, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 9.dp))
        Text(item.summary, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Text(practiceHistoryText(item.practiceCount, item.lastPracticedAt), color = LocalPrepColors.current.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun PracticeFilterLabel(text: String, color: Color) {
    Text(text, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp), fontWeight = FontWeight.Bold, color = color)
}

@Composable
private fun EmptyPracticeState(text: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        Surface(onClick = onAction, color = LocalPrepColors.current.primary, shape = RoundedCornerShape(50)) {
            Text("立即导入", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        }
    }
}
