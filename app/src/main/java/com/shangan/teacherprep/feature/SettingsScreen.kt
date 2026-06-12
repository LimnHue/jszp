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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
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
import com.shangan.teacherprep.data.PaletteStyle
import com.shangan.teacherprep.data.ScopeConfig
import com.shangan.teacherprep.data.TimerMode
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.ScreenHeader
import kotlin.math.roundToInt

private enum class ConfigField(val label: String) {
    TEXTBOOK("教材"),
    GENRE("题材"),
    STRUCTURED("结构化种类"),
    TEMPLATE("模板种类"),
    TRIAL_SECTION("试讲内容结构"),
    STRUCTURED_SECTION("结构化内容结构"),
}

@Composable
fun SettingsScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onPreferences: (TimerMode?, Int?, Int?, PaletteStyle?, Float?) -> Unit,
    onUpdateConfig: ((ScopeConfig) -> ScopeConfig) -> Unit,
    onImportBackup: () -> Unit,
    onExport: (Boolean) -> Unit,
) {
    val prefs = data.preferences
    val config = data.scopeConfigs[prefs.selectedScope.key] ?: ScopeConfig()
    var editing by remember { mutableStateOf<ConfigField?>(null) }
    var trialMinutes by remember(prefs.defaultTrialMinutes) { mutableFloatStateOf(prefs.defaultTrialMinutes.toFloat()) }
    var structuredMinutes by remember(prefs.defaultStructuredMinutes) { mutableFloatStateOf(prefs.defaultStructuredMinutes.toFloat()) }
    var opacity by remember(prefs.surfaceOpacity) { mutableFloatStateOf(prefs.surfaceOpacity) }

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("设置") }
        item {
            SettingsTitle("练习计时")
            RoundedCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
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
        item {
            SettingsTitle("主题配色")
            RoundedCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
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
        item {
            SettingsTitle("当前题库分类")
            ConfigEditor(config, onEdit = { editing = it }, onRemove = { field, value ->
                onUpdateConfig { removeValue(it, field, value) }
            })
        }
        item {
            SettingsTitle("备考库迁移")
            RoundedCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                ActionRow(Icons.Rounded.Share, "分享当前题库", "仅导出 ${prefs.selectedScope.stage} · ${prefs.selectedScope.subject}") { onExport(false) }
                ActionRow(Icons.Rounded.Download, "导出完整备考库", "包含所有学段、学科和设置") { onExport(true) }
                ActionRow(Icons.Rounded.UploadFile, "导入备考库", "按内容 ID 合并，不覆盖本地资料", onImportBackup)
            }
        }
    }

    editing?.let { field ->
        AddConfigDialog(field.label, onDismiss = { editing = null }) { value ->
            onUpdateConfig { addValue(it, field, value) }
            editing = null
        }
    }
}

@Composable
private fun SettingsTitle(text: String) {
    Text(text, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp), fontSize = 20.sp, fontWeight = FontWeight.Black)
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
        ConfigField.GENRE to config.genres,
        ConfigField.STRUCTURED to config.structuredTypes,
        ConfigField.TEMPLATE to config.templateTypes,
        ConfigField.TRIAL_SECTION to config.trialSectionNames,
        ConfigField.STRUCTURED_SECTION to config.structuredSectionNames,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEach { (field, values) ->
            RoundedCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
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
    ConfigField.GENRE -> config.copy(genres = (config.genres + value).distinct())
    ConfigField.STRUCTURED -> config.copy(structuredTypes = (config.structuredTypes + value).distinct())
    ConfigField.TEMPLATE -> config.copy(templateTypes = (config.templateTypes + value).distinct())
    ConfigField.TRIAL_SECTION -> config.copy(trialSectionNames = (config.trialSectionNames + value).distinct())
    ConfigField.STRUCTURED_SECTION -> config.copy(structuredSectionNames = (config.structuredSectionNames + value).distinct())
}

private fun removeValue(config: ScopeConfig, field: ConfigField, value: String): ScopeConfig = when (field) {
    ConfigField.TEXTBOOK -> config.copy(textbooks = config.textbooks - value)
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
