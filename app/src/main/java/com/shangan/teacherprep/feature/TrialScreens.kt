package com.shangan.teacherprep.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.shangan.teacherprep.data.PracticeModule
import com.shangan.teacherprep.data.TrialLesson
import com.shangan.teacherprep.ui.BatchActionBar
import com.shangan.teacherprep.ui.FilterChips
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.ui.BrandMark
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
import kotlinx.coroutines.launch

@Composable
fun TrialLibraryScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onSwitchScope: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteBatch: (Set<String>) -> Unit,
    onExportBatch: (Set<String>) -> Unit,
    onImport: () -> Unit,
    onUpdateDrawSelections: (PracticeModule, Map<String, Set<String>>) -> Unit,
) {
    val scope = data.preferences.selectedScope
    val filterVisibility = data.preferences.filterVisibility
    val config = data.scopeConfigs[scope.key] ?: com.shangan.teacherprep.data.ScopeDefaults.create(scope)
    var query by remember { mutableStateOf("") }
    var textbook by remember { mutableStateOf("全部") }
    var unit by remember { mutableStateOf("全部") }
    var genre by remember { mutableStateOf("全部") }
    var importance by remember { mutableStateOf("全部") }
    var showDrawDialog by remember { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val scopeItems = data.trials.filter { it.scopeKey == scope.key }
    val drawGroups = listOf(
        RandomDrawGroup("textbook", "教材", scopeItems.map { it.textbook }.distinct()),
        RandomDrawGroup("unit", "单元", scopeItems.map { it.unit }.filter { it.isNotBlank() }.distinct()),
        RandomDrawGroup("genre", "题材", scopeItems.map { it.genre }.distinct()),
        RandomDrawGroup("importance", "重要程度", scopeItems.map { "${it.importance} 星" }.distinct().sortedDescending()),
    )
    val drawCandidates = scopeItems.map {
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
    val drawSelections = savedRandomDrawSelections(data.preferences, scope.key, PracticeModule.TRIAL)
    val items = scopeItems
        .filter {
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
    val listState = rememberLazyListState()
    val visibleIds = remember(items) { items.map { it.id }.toSet() }

    LaunchedEffect(visibleIds) {
        selectedIds = selectedIds.intersect(visibleIds)
        if (visibleIds.isEmpty()) batchMode = false
    }

    Scaffold(
        containerColor = Color(0xFFF7F5F1),
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
            state = listState,
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
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFE9EEF2)) {
                                Icon(Icons.Rounded.Shuffle, null, tint = LocalPrepColors.current.primary, modifier = Modifier.padding(11.dp).size(25.dp))
                            }
                            Column(Modifier.padding(start = 18.dp)) {
                                Text("随机抽课", color = Color(0xFF202224), fontSize = 20.sp, fontWeight = FontWeight.Black)
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
                if (batchMode) {
                    item {
                        BatchActionBar(
                            selectedCount = selectedIds.size,
                            totalCount = items.size,
                            itemLabel = "试讲",
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
                items(items, key = { it.id }) { item ->
                    TrialCard(
                        item = item,
                        selectionMode = batchMode,
                        selected = item.id in selectedIds,
                        onSelectionChange = { selected ->
                            batchMode = true
                            selectedIds = if (selected) selectedIds + item.id else selectedIds - item.id
                        },
                        onOpen = onOpen,
                        onEdit = onEdit,
                        onDelete = onDelete,
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
            title = "按标签抽试讲",
            groups = drawGroups,
            candidates = drawCandidates,
            initialSelections = drawSelections,
            onDismiss = { showDrawDialog = false },
            onSave = {
                onUpdateDrawSelections(PracticeModule.TRIAL, it)
                showDrawDialog = false
            },
        )
    }
}

@Composable
private fun TrialCard(
    item: TrialLesson,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    EditableSwipeCard(
        itemLabel = lessonDisplayTitle(item),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        selectionMode = selectionMode,
        selected = selected,
        onSelectionChange = onSelectionChange,
        onOpen = { onOpen(item.id) },
        onEdit = { onEdit(item.id) },
        onDelete = { onDelete(item.id) },
        containerColor = Color.White,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).background(LocalPrepColors.current.primary.copy(alpha = .08f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                BrandMark(size = 32)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(lessonDisplayTitle(item), fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(
                    listOf(item.textbook, item.unit, item.genre).filter { it.isNotBlank() }.joinToString(" · "),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    ImportanceStars(item.importance, iconSize = 13)
                    Text("  ${item.durationMinutes}分钟", fontSize = 12.sp, color = Color.Gray)
                }
                Text(
                    practiceHistoryText(item.practiceCount, item.lastPracticedAt),
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrialDetailScreen(
    lesson: TrialLesson,
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
    val labels = listOf("课程信息", "试讲流程", "板书设计", "练习记录")
    val pageToTab = listOf(0, 1, 2, 3)
    val pagerState = rememberPagerState(
        initialPage = preferences.defaultTrialStartPage.ordinal.coerceIn(0, 3),
        pageCount = { 4 },
    )
    val listStates = listOf(
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
    )
    val selectedTab = pageToTab[pagerState.currentPage]
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenHeader(
                title = lesson.title,
                onBack = onBack,
                action = {
                    Row {
                        IconButton(onClick = onExport) {
                            Icon(Icons.Rounded.Share, contentDescription = "导出课程", tint = LocalPrepColors.current.primary)
                        }
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
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFFFCFBF8)) {
                labels.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(pageToTab.indexOf(index)) } },
                        text = { Text(label, fontWeight = FontWeight.Bold) },
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val tab = pageToTab[page]
                    val listState = listStates[tab]
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF7F5F1)),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when (tab) {
                                0 -> item {
                                    RoundedCard(containerColor = Color.White) {
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
                                    RoundedCard(containerColor = Color.White) {
                                        Text("${index + 1}、${section.title}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF202126))
                                        Spacer(Modifier.height(12.dp))
                                        MarkdownText(section.markdown)
                                    }
                                }
                                2 -> item {
                                    RoundedCard(containerColor = Color.White) {
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
                                    TrialMediaSection(
                                        lesson = lesson,
                                        onMediaSaved = onMediaSaved,
                                        onMediaDelete = onMediaDelete,
                                        onMediaRename = onMediaRename,
                                    )
                                }
                            }
                        }
                        DraggableScrollToTopButton(
                            listState,
                            Modifier.padding(bottom = padding.calculateBottomPadding()),
                        )
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
