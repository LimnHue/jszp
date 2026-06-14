package com.shangan.teacherprep.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.FilterVisibility
import com.shangan.teacherprep.data.PaletteStyle
import com.shangan.teacherprep.data.ScopeConfig
import com.shangan.teacherprep.data.TimerMode
import com.shangan.teacherprep.data.TrialStartPage
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.ui.ScreenHeader
import com.shangan.teacherprep.ui.observeHorizontalSwipe
import com.shangan.teacherprep.ui.theme.LocalPrepColors
import kotlin.math.roundToInt

private enum class ConfigField(val label: String) {
    TEXTBOOK("教材"),
    UNIT("单元"),
    GENRE("题材"),
    STRUCTURED("结构化种类"),
    TEMPLATE("模板种类"),
    TRIAL_SECTION("试讲流程结构"),
    STRUCTURED_SECTION("结构化内容结构"),
}

enum class SettingsSection(val title: String, val subtitle: String) {
    TIMER("练习计时", "计时方式、默认时长与试讲打开页面"),
    VOICE("语音提示", "提前提醒和练习结束提示"),
    FILTERS("筛选显示", "控制各题库页面显示哪些筛选项"),
    APPEARANCE("主题外观", "卡片、字体、Logo 大小与透明度"),
    FORMAT_GUIDE("导入格式范式", "Markdown、TXT 与 Word 文档的写法示例"),
    LIBRARY("题库分类", "教材、单元、题材和内容结构"),
    BACKUP("备份与迁移", "导入、导出或分享备考库"),
}

@Composable
fun SettingsScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onOpenSection: (SettingsSection) -> Unit,
) {
    val listState = rememberLazyListState()
    val accent = LocalPrepColors.current.primary
    val entries = listOf(
        Triple(SettingsSection.TIMER, Icons.Rounded.Timer, accent),
        Triple(SettingsSection.VOICE, Icons.Rounded.NotificationsActive, accent),
        Triple(SettingsSection.FILTERS, Icons.Rounded.FilterAlt, accent),
        Triple(SettingsSection.APPEARANCE, Icons.Rounded.Palette, accent),
        Triple(SettingsSection.FORMAT_GUIDE, Icons.Rounded.Description, accent),
        Triple(SettingsSection.LIBRARY, Icons.Rounded.Tune, accent),
        Triple(SettingsSection.BACKUP, Icons.Rounded.Storage, accent),
    )
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
        state = listState,
        modifier = Modifier.background(Color(0xFFF7F5F1)),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item { ScreenHeader("设置") }
        item {
            Text(
                "${data.preferences.selectedScope.stage} · ${data.preferences.selectedScope.subject} · ${data.preferences.selectedScope.textbookVersion}",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                color = Color.Gray,
            )
        }
        items(entries.size) { index ->
            val (section, icon, color) = entries[index]
            Surface(
                onClick = { onOpenSection(section) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFCFBF8),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(15.dp), color = color.copy(alpha = .12f)) {
                        Icon(icon, null, tint = color, modifier = Modifier.padding(12.dp).size(27.dp))
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(section.title, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text(section.subtitle, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    Icon(Icons.Rounded.ChevronRight, "进入${section.title}", tint = Color.LightGray)
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

@Composable
fun SettingsDetailScreen(
    section: SettingsSection,
    data: AppData,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPreferences: (TimerMode?, Int?, Int?, PaletteStyle?, Float?) -> Unit,
    onAppearance: (Float, Float, Float, Float) -> Unit,
    onTrialStartPage: (TrialStartPage) -> Unit,
    onReminderPreferences: (Boolean?, Int?, Boolean?) -> Unit,
    onFilterVisibility: ((FilterVisibility) -> FilterVisibility) -> Unit,
    onUpdateConfig: ((ScopeConfig) -> ScopeConfig) -> Unit,
    onImportBackup: () -> Unit,
    onExport: (Boolean) -> Unit,
) {
    val prefs = data.preferences
    val config = data.scopeConfigs[prefs.selectedScope.key] ?: com.shangan.teacherprep.data.ScopeDefaults.create(prefs.selectedScope)
    var editing by remember { mutableStateOf<ConfigField?>(null) }
    var trialMinutes by remember(prefs.defaultTrialMinutes) { mutableFloatStateOf(prefs.defaultTrialMinutes.toFloat()) }
    var structuredMinutes by remember(prefs.defaultStructuredMinutes) { mutableFloatStateOf(prefs.defaultStructuredMinutes.toFloat()) }
    var reminderMinutes by remember(prefs.reminderMinutesBeforeEnd) {
        mutableFloatStateOf(prefs.reminderMinutesBeforeEnd.toFloat())
    }
    var opacity by remember(prefs.surfaceOpacity) { mutableFloatStateOf(prefs.surfaceOpacity) }
    var logoScale by remember(prefs.logoScale) { mutableFloatStateOf(prefs.logoScale) }
    var uiScale by remember(prefs.uiScale) { mutableFloatStateOf(prefs.uiScale) }
    var fontScale by remember(prefs.fontScale) { mutableFloatStateOf(prefs.fontScale) }
    val latestOpacity by rememberUpdatedState(opacity)
    val latestLogoScale by rememberUpdatedState(logoScale)
    val latestUiScale by rememberUpdatedState(uiScale)
    val latestFontScale by rememberUpdatedState(fontScale)
    val listState = rememberLazyListState()

    DisposableEffect(section) {
        onDispose {
            if (section == SettingsSection.APPEARANCE) {
                onAppearance(latestOpacity, latestLogoScale, latestUiScale, latestFontScale)
            }
        }
    }

    Box(Modifier.fillMaxSize().observeHorizontalSwipe(onSwipeLeft = onBack)) {
        LazyColumn(
        state = listState,
        modifier = Modifier.background(Color(0xFFF7F5F1)),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item { ScreenHeader(section.title, onBack = onBack) }
        if (section == SettingsSection.TIMER) item {
            SettingsTitle("练习计时", LocalPrepColors.current.primary)
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFCFBF8),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimerMode.entries.forEach { mode ->
                        Row(
                            Modifier.weight(1f).clickable { onPreferences(mode, null, null, null, null) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = prefs.timerMode == mode, onClick = { onPreferences(mode, null, null, null, null) })
                            Text(if (mode == TimerMode.COUNTDOWN) "倒计时" else "正计时")
                        }
                    }
                }
                SliderSetting("试讲默认时长", trialMinutes, 5f..40f, "分钟") {
                    trialMinutes = it
                    onPreferences(null, it.roundToInt(), null, null, null)
                }
                SliderSetting("结构化默认时长", structuredMinutes, 1f..15f, "分钟") {
                    structuredMinutes = it
                    onPreferences(null, null, it.roundToInt(), null, null)
                }
                Text(
                    "试讲默认打开页面",
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                )
                TrialStartPage.entries.forEach { page ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onTrialStartPage(page) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = prefs.defaultTrialStartPage == page,
                            onClick = { onTrialStartPage(page) },
                        )
                        Column(Modifier.padding(vertical = 5.dp)) {
                            Text(trialStartPageLabel(page), fontWeight = FontWeight.Bold)
                            if (page == TrialStartPage.COURSE_INFO) {
                                Text("默认选项", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        if (section == SettingsSection.VOICE) item {
            SettingsTitle("语音提醒", LocalPrepColors.current.primary)
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFCFBF8),
            ) {
                ToggleSetting(
                    title = "倒计时提前提醒",
                    subtitle = "剩余指定时间时进行语音播报",
                    checked = prefs.remindBeforeEnd,
                    onCheckedChange = { onReminderPreferences(it, null, null) },
                )
                if (prefs.remindBeforeEnd) {
                    SliderSetting("提前提醒时间", reminderMinutes, 1f..15f, "分钟") {
                        reminderMinutes = it
                        onReminderPreferences(null, it.roundToInt(), null)
                    }
                }
                ToggleSetting(
                    title = "倒计时结束提醒",
                    subtitle = "时间归零时播报练习结束",
                    checked = prefs.remindAtEnd,
                    onCheckedChange = { onReminderPreferences(null, null, it) },
                )
                Text(
                    "语音提醒仅在倒计时模式生效；设备没有系统语音服务时会自动播放内置提示音。",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (section == SettingsSection.FILTERS) item {
            SettingsTitle("筛选显示", LocalPrepColors.current.primary)
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFCFBF8),
            ) {
                Text("试讲", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                FilterToggle("搜索框", prefs.filterVisibility.trialSearch) {
                    onFilterVisibility { visibility -> visibility.copy(trialSearch = it) }
                }
                FilterToggle("教材", prefs.filterVisibility.trialTextbook) {
                    onFilterVisibility { visibility -> visibility.copy(trialTextbook = it) }
                }
                FilterToggle("单元", prefs.filterVisibility.trialUnit) {
                    onFilterVisibility { visibility -> visibility.copy(trialUnit = it) }
                }
                FilterToggle("题材", prefs.filterVisibility.trialGenre) {
                    onFilterVisibility { visibility -> visibility.copy(trialGenre = it) }
                }
                FilterToggle("重要程度", prefs.filterVisibility.trialImportance) {
                    onFilterVisibility { visibility -> visibility.copy(trialImportance = it) }
                }
                Text("结构化", fontWeight = FontWeight.Black, color = LocalPrepColors.current.primary, modifier = Modifier.padding(top = 10.dp))
                FilterToggle("问题种类", prefs.filterVisibility.structuredCategory) {
                    onFilterVisibility { visibility -> visibility.copy(structuredCategory = it) }
                }
                FilterToggle("重要程度", prefs.filterVisibility.structuredImportance) {
                    onFilterVisibility { visibility -> visibility.copy(structuredImportance = it) }
                }
                Text("模板", fontWeight = FontWeight.Black, color = LocalPrepColors.current.primary, modifier = Modifier.padding(top = 10.dp))
                FilterToggle("搜索框", prefs.filterVisibility.templateSearch) {
                    onFilterVisibility { visibility -> visibility.copy(templateSearch = it) }
                }
                FilterToggle("模板种类", prefs.filterVisibility.templateCategory) {
                    onFilterVisibility { visibility -> visibility.copy(templateCategory = it) }
                }
            }
        }
        if (section == SettingsSection.APPEARANCE) item {
            SettingsTitle("界面外观", LocalPrepColors.current.primary)
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFCFBF8),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color(0xFF52677D), Color(0xFFE9EEF2), Color(0xFFF7F5F1)).forEach { color ->
                            Surface(
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                color = color,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
                            ) {}
                        }
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("静谧灰蓝", fontWeight = FontWeight.Black)
                        Text("暖白底色、炭黑文字与统一灰蓝强调色", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                SliderSetting("卡片透明度", opacity * 100, 55f..100f, "%") {
                    opacity = it / 100
                }
                Text(
                    "100% 为当前手机推荐尺寸",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                SliderSetting("卡片与控件大小", uiScale * 100, 75f..120f, "%") {
                    uiScale = it / 100
                }
                SliderSetting("字体大小", fontScale * 100, 80f..120f, "%") {
                    fontScale = it / 100
                }
                SliderSetting("Logo 大小", logoScale * 100, 70f..140f, "%") {
                    logoScale = it / 100
                }
                Text(
                    "调整会在退出本页面后统一生效",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (section == SettingsSection.FORMAT_GUIDE) {
            item {
                SettingsTitle("支持的文件", LocalPrepColors.current.primary)
                RoundedCard(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    containerColor = Color(0xFFFCFBF8),
                ) {
                    Text(".md", fontWeight = FontWeight.Black)
                    Text("推荐格式。用 # 标题划分章节，用 - 或 1. 表示要点。", color = Color.Gray)
                    Text(".txt", fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
                    Text("可使用与 Markdown 相同的写法；也可用单独一行的章节名称分段。", color = Color.Gray)
                    Text(".docx", fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
                    Text("Word 中每个章节标题单独占一行，正文写在标题下方。旧版 .doc 请先另存为 .docx。", color = Color.Gray)
                }
            }
            item {
                FormatGuideCard(
                    title = "试讲课程范式",
                    description = "课程名称、教材、单元、题材、课时与星级在导入页面选择。文档主要填写课程信息和试讲流程。",
                    template = """
# 课程信息
- 教学目标：理解文章内容，体会作者情感
- 教学重点：品味重点语句
- 教学难点：理解写作手法

# 导入新课
- 从生活情境或图片导入
- 提出本课核心问题

# 整体感知
1. 自由朗读，概括主要内容
2. 圈画关键词句

# 品读赏析
- 问题一：这句话写出了什么？
  - 关注修辞和关键词
- 问题二：表达了怎样的情感？

# 课堂小结
- 回扣教学目标
- 布置课后练习
                    """.trimIndent(),
                )
            }
            item {
                FormatGuideCard(
                    title = "结构化问题范式",
                    description = "问题本身填写在导入页的“结构化问题”栏；文档按答题思路和参考答案分段。",
                    template = """
# 答题思路
1. 明确问题中的矛盾与目标
2. 分析原因和影响
3. 提出具体、可执行的措施

# 参考答案
各位考官，面对这一情况，我会保持冷静，
先了解事实，再分别沟通，最后跟进反馈。
                    """.trimIndent(),
                )
            }
            item {
                FormatGuideCard(
                    title = "答题模板范式",
                    description = "模板名称和种类在导入页面选择。正文可自由分章，也可只写一个完整框架。",
                    template = """
# 开头
各位考官，我认为这一问题需要客观看待。

# 分析
- 从背景和原因展开
- 说明积极意义或潜在问题

# 对策
1. 完善制度
2. 加强沟通
3. 做好监督与反馈

# 结尾
作为一名教师，我会把以上措施落实到工作中。
                    """.trimIndent(),
                )
            }
            item {
                RoundedCard(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    containerColor = Color(0xFFE9EEF2),
                ) {
                    Text("导入步骤", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "1. 按上方范式整理文档。\n2. 在试讲、结构化或模板页点击“导入”。\n3. 选择文档后检查自动分段。\n4. 补充教材、分类、星级等信息并保存。",
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 22.sp,
                    )
                }
            }
        }
        if (section == SettingsSection.LIBRARY) item {
            SettingsTitle("当前题库分类", LocalPrepColors.current.primary)
            ConfigEditor(config, onEdit = { editing = it }, onRemove = { field, value ->
                onUpdateConfig { removeValue(it, field, value) }
            })
        }
        if (section == SettingsSection.BACKUP) item {
            SettingsTitle("备考库迁移", LocalPrepColors.current.primary)
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFCFBF8),
            ) {
                ActionRow(
                    Icons.Rounded.Share,
                    "分享当前题库",
                    "仅导出 ${prefs.selectedScope.stage} · ${prefs.selectedScope.subject} · ${prefs.selectedScope.textbookVersion}",
                ) { onExport(false) }
                ActionRow(Icons.Rounded.Download, "导出完整备考库", "包含所有学段、学科和设置") { onExport(true) }
                ActionRow(Icons.Rounded.UploadFile, "导入备考库", "按内容 ID 合并，不覆盖本地资料", onImportBackup)
            }
        }
        }
        DraggableScrollToTopButton(listState)
    }

    editing?.let { field ->
        AddConfigDialog(field.label, onDismiss = { editing = null }) { value ->
            onUpdateConfig { addValue(it, field, value) }
            editing = null
        }
    }
}

private fun trialStartPageLabel(page: TrialStartPage): String = when (page) {
    TrialStartPage.COURSE_INFO -> "课程信息"
    TrialStartPage.TRIAL_FLOW -> "试讲流程"
    TrialStartPage.BOARD_DESIGN -> "板书设计"
    TrialStartPage.PRACTICE_RECORDS -> "练习记录"
}

@Composable
private fun SettingsTitle(text: String, color: Color = Color(0xFF202126)) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 5.dp, height = 22.dp).background(color, RoundedCornerShape(50)))
        Text(
            text,
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color,
        )
    }
}

@Composable
private fun SliderSetting(label: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("${value.roundToInt()}$unit", color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range)
}

@Composable
private fun FormatGuideCard(
    title: String,
    description: String,
    template: String,
) {
    RoundedCard(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        containerColor = Color(0xFFFCFBF8),
    ) {
        Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(
            description,
            color = Color.Gray,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F3F4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD9D2)),
        ) {
            SelectionContainer {
                Text(
                    template,
                    modifier = Modifier.padding(14.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF34383C),
                )
            }
        }
    }
}

@Composable
private fun ConfigEditor(
    config: ScopeConfig,
    onEdit: (ConfigField) -> Unit,
    onRemove: (ConfigField, String) -> Unit,
) {
    val groups = listOf(
        ConfigField.TEXTBOOK to config.textbooks,
        ConfigField.UNIT to config.units,
        ConfigField.GENRE to config.genres,
        ConfigField.STRUCTURED to config.structuredTypes,
        ConfigField.TEMPLATE to config.templateTypes,
        ConfigField.TRIAL_SECTION to config.trialSectionNames,
        ConfigField.STRUCTURED_SECTION to config.structuredSectionNames,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEachIndexed { index, (field, values) ->
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFCFBF8),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(field.label, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onEdit(field) }) { Icon(Icons.Rounded.Add, "添加") }
                }
                values.forEach { value ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(value, Modifier.weight(1f).padding(vertical = 7.dp), color = Color(0xFF55565D))
                        IconButton(onClick = { onRemove(field, value) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, "删除", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .1f)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = Color.Gray, fontSize = 12.sp) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FilterToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ToggleSetting(label, checked = checked, onCheckedChange = onCheckedChange)
}

@Composable
private fun AddConfigDialog(label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增$label") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("名称") }) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun addValue(config: ScopeConfig, field: ConfigField, value: String): ScopeConfig = when (field) {
    ConfigField.TEXTBOOK -> config.copy(textbooks = (config.textbooks + value).distinct())
    ConfigField.UNIT -> config.copy(units = (config.units + value).distinct())
    ConfigField.GENRE -> config.copy(genres = (config.genres + value).distinct())
    ConfigField.STRUCTURED -> config.copy(structuredTypes = (config.structuredTypes + value).distinct())
    ConfigField.TEMPLATE -> config.copy(templateTypes = (config.templateTypes + value).distinct())
    ConfigField.TRIAL_SECTION -> config.copy(trialSectionNames = (config.trialSectionNames + value).distinct())
    ConfigField.STRUCTURED_SECTION -> config.copy(structuredSectionNames = (config.structuredSectionNames + value).distinct())
}

private fun removeValue(config: ScopeConfig, field: ConfigField, value: String): ScopeConfig = when (field) {
    ConfigField.TEXTBOOK -> config.copy(textbooks = config.textbooks - value)
    ConfigField.UNIT -> config.copy(units = config.units - value)
    ConfigField.GENRE -> config.copy(genres = config.genres - value)
    ConfigField.STRUCTURED -> config.copy(structuredTypes = config.structuredTypes - value)
    ConfigField.TEMPLATE -> config.copy(templateTypes = config.templateTypes - value)
    ConfigField.TRIAL_SECTION -> config.copy(trialSectionNames = config.trialSectionNames - value)
    ConfigField.STRUCTURED_SECTION -> config.copy(structuredSectionNames = config.structuredSectionNames - value)
}
