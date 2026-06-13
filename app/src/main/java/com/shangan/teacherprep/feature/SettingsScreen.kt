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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.FilterVisibility
import com.shangan.teacherprep.data.PaletteStyle
import com.shangan.teacherprep.data.ScopeConfig
import com.shangan.teacherprep.data.TimerMode
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.DraggableScrollToTopButton
import com.shangan.teacherprep.ui.ScreenHeader
import com.shangan.teacherprep.ui.observeHorizontalSwipe
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
    TIMER("练习计时", "倒计时、正计时与默认练习时长"),
    VOICE("语音提示", "提前提醒和练习结束提示"),
    FILTERS("筛选显示", "控制各题库页面显示哪些筛选项"),
    APPEARANCE("主题外观", "主题配色与卡片透明度"),
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
    val entries = listOf(
        Triple(SettingsSection.TIMER, Icons.Rounded.Timer, Color(0xFF287BDE)),
        Triple(SettingsSection.VOICE, Icons.Rounded.NotificationsActive, Color(0xFFE76A3B)),
        Triple(SettingsSection.FILTERS, Icons.Rounded.FilterAlt, Color(0xFF6B50D8)),
        Triple(SettingsSection.APPEARANCE, Icons.Rounded.Palette, Color(0xFFD94E89)),
        Triple(SettingsSection.LIBRARY, Icons.Rounded.Tune, Color(0xFF138C75)),
        Triple(SettingsSection.BACKUP, Icons.Rounded.Storage, Color(0xFFD87516)),
    )
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
        state = listState,
        modifier = Modifier.background(Color(0xFFF7F9FC)),
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
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 2.dp,
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
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().observeHorizontalSwipe(onSwipeLeft = onBack)) {
        LazyColumn(
        state = listState,
        modifier = Modifier.background(Color(0xFFF7F9FC)),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item { ScreenHeader(section.title, onBack = onBack) }
        if (section == SettingsSection.TIMER) item {
            SettingsTitle("练习计时", Color(0xFF287BDE))
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFF1F7FF),
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
            }
        }
        if (section == SettingsSection.VOICE) item {
            SettingsTitle("语音提醒", Color(0xFFE76A3B))
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFFF5F0),
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
            SettingsTitle("筛选显示", Color(0xFF6B50D8))
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFF7F4FF),
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
                Text("结构化", fontWeight = FontWeight.Black, color = Color(0xFF7654F6), modifier = Modifier.padding(top = 10.dp))
                FilterToggle("问题种类", prefs.filterVisibility.structuredCategory) {
                    onFilterVisibility { visibility -> visibility.copy(structuredCategory = it) }
                }
                FilterToggle("重要程度", prefs.filterVisibility.structuredImportance) {
                    onFilterVisibility { visibility -> visibility.copy(structuredImportance = it) }
                }
                Text("模板", fontWeight = FontWeight.Black, color = Color(0xFFFF6F1A), modifier = Modifier.padding(top = 10.dp))
                FilterToggle("搜索框", prefs.filterVisibility.templateSearch) {
                    onFilterVisibility { visibility -> visibility.copy(templateSearch = it) }
                }
                FilterToggle("模板种类", prefs.filterVisibility.templateCategory) {
                    onFilterVisibility { visibility -> visibility.copy(templateCategory = it) }
                }
            }
        }
        if (section == SettingsSection.APPEARANCE) item {
            SettingsTitle("主题配色", Color(0xFFD94E89))
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFFF3F8),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PaletteStyle.entries.forEach { style ->
                        val color = palettePreview(style)
                        Box(
                            Modifier.size(if (prefs.palette == style) 48.dp else 40.dp)
                                .background(color, CircleShape)
                                .clickable { onPreferences(null, null, null, style, null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (prefs.palette == style) Text("✓", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }
                SliderSetting("卡片透明度", opacity * 100, 55f..100f, "%") {
                    opacity = it / 100
                    onPreferences(null, null, null, null, opacity)
                }
            }
        }
        if (section == SettingsSection.LIBRARY) item {
            SettingsTitle("当前题库分类", Color(0xFF138C75))
            ConfigEditor(config, onEdit = { editing = it }, onRemove = { field, value ->
                onUpdateConfig { removeValue(it, field, value) }
            })
        }
        if (section == SettingsSection.BACKUP) item {
            SettingsTitle("备考库迁移", Color(0xFFD87516))
            RoundedCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                containerColor = Color(0xFFFFF7EA),
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
                containerColor = if (index % 2 == 0) Color(0xFFF0FAF7) else Color(0xFFF5FBF9),
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

private fun palettePreview(style: PaletteStyle): Color = when (style) {
    PaletteStyle.CORAL -> Color(0xFFFF3150)
    PaletteStyle.SKY -> Color(0xFF248BFF)
    PaletteStyle.PINK -> Color(0xFFFF5B9B)
    PaletteStyle.VIOLET -> Color(0xFF7654F6)
    PaletteStyle.MINT -> Color(0xFF18A88B)
}
