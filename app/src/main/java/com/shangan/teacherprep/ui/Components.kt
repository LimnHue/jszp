package com.shangan.teacherprep.ui

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shangan.teacherprep.data.AppPreferences
import com.shangan.teacherprep.data.LibraryScope
import com.shangan.teacherprep.data.TimerMode
import com.shangan.teacherprep.ui.theme.LocalPrepColors
import com.shangan.teacherprep.ui.theme.LocalSurfaceOpacity
import kotlinx.coroutines.delay
import java.util.Locale

enum class MainDestination(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Rounded.Home),
    TRIAL("试讲", Icons.Rounded.Slideshow),
    STRUCTURED("结构化", Icons.Rounded.ChatBubbleOutline),
    TEMPLATE("模板", Icons.Rounded.Layers),
    SETTINGS("设置", Icons.Rounded.Settings),
}

@Composable
fun PrepBottomBar(selected: MainDestination, onSelect: (MainDestination) -> Unit) {
    val colors = LocalPrepColors.current
    NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
        MainDestination.entries.forEach { item ->
            NavigationBarItem(
                selected = selected == item,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary.copy(alpha = .1f),
                ),
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    scope: LibraryScope? = null,
    onScopeClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "返回") }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        if (scope != null && onScopeClick != null) {
            ScopePill(scope, onScopeClick)
        }
        action?.invoke()
    }
}

@Composable
fun ScopePill(scope: LibraryScope, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${scope.stage} · ${scope.subject} · ${scope.textbookVersion}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "切换题库", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FilterChips(
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    includeAll: Boolean = true,
    horizontalPadding: Int = 20,
) {
    val options = if (includeAll) listOf("全部") + values else values
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = horizontalPadding.dp),
    ) {
        items(options.distinct()) { item ->
            val active = item == selected
            Surface(
                onClick = { onSelect(item) },
                shape = RoundedCornerShape(14.dp),
                color = if (active) LocalPrepColors.current.primary else Color(0xFFF5F4F5),
                contentColor = if (active) Color.White else Color(0xFF5F6068),
            ) {
                Text(item, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun ImportanceStars(
    value: Int,
    modifier: Modifier = Modifier,
    onValueChange: ((Int) -> Unit)? = null,
    activeColor: Color = Color(0xFFFFB000),
    inactiveColor: Color = Color(0xFFCACAD0),
    iconSize: Int = 24,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= value.coerceIn(1, 5)) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (onValueChange == null) null else "$star 星",
                tint = if (star <= value.coerceIn(1, 5)) activeColor else inactiveColor,
                modifier = Modifier.size(iconSize.dp).then(
                    if (onValueChange == null) Modifier else Modifier.clickable { onValueChange(star) },
                ),
            )
        }
    }
}

@Composable
fun GradientActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    start: Color = LocalPrepColors.current.primary,
    end: Color = LocalPrepColors.current.gradientEnd,
) {
    Button(
        onClick = onClick,
        modifier = modifier.shadow(10.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(start, end)))
                .padding(horizontal = 24.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

@Composable
fun ModuleImportEntry(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalPrepColors.current.primary,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = .09f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = color)
            Text(text, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
            Text("新建或导入文档", color = color.copy(alpha = .75f), fontSize = 12.sp)
        }
    }
}

@Composable
fun RoundedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.White.copy(alpha = LocalSurfaceOpacity.current),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.animateContentSize().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun SectionPanel(
    modifier: Modifier = Modifier,
    color: Color = LocalPrepColors.current.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = .075f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .16f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        markdown.lines().forEach { raw ->
            val line = raw.trim()
            val leading = raw.takeWhile { it == ' ' || it == '\t' }
            val indent = leading.fold(0) { total, character -> total + if (character == '\t') 4 else 1 } / 4
            when {
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                line.startsWith("# ") -> Text(line.removePrefix("# "), fontSize = 24.sp, fontWeight = FontWeight.Black)
                line.startsWith("## ") -> Text(line.removePrefix("## "), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                line.startsWith("### ") -> Text(line.removePrefix("### "), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                line.startsWith("> ") -> Surface(
                    color = LocalPrepColors.current.primary.copy(alpha = .08f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(line.removePrefix("> "), modifier = Modifier.padding(14.dp), color = LocalPrepColors.current.primary)
                }
                Regex("""^(\d+\.|[-*])\s+""").containsMatchIn(line) ->
                    Text(
                        "• " + line.replaceFirst(Regex("""^(\d+\.|[-*])\s+"""), ""),
                        modifier = Modifier.padding(start = (indent * 20).dp),
                        lineHeight = 25.sp,
                        color = if (indent > 0) Color(0xFF55565D) else Color(0xFF33343A),
                    )
                else -> Text(line, lineHeight = 26.sp, color = Color(0xFF33343A))
            }
        }
    }
}

@Composable
fun PracticeTimer(
    preferences: AppPreferences,
    minutes: Int,
    modifier: Modifier = Modifier,
    onPracticeStarted: () -> Unit = {},
) {
    val mode = preferences.timerMode
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var running by remember { mutableStateOf(false) }
    var hasStarted by remember(mode, minutes) { mutableStateOf(false) }
    var spokenBeforeEnd by remember(mode, minutes) { mutableStateOf(false) }
    var spokenAtEnd by remember(mode, minutes) { mutableStateOf(false) }
    var seconds by remember(mode, minutes) {
        mutableLongStateOf(if (mode == TimerMode.COUNTDOWN) minutes * 60L else 0L)
    }
    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine.language = Locale.CHINA
                textToSpeech = engine
            }
        }
        onDispose {
            engine.stop()
            engine.shutdown()
            textToSpeech = null
        }
    }
    fun playBundledReminder(resourceName: String): Boolean {
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resourceId == 0) return false
        return runCatching {
            MediaPlayer.create(context.applicationContext, resourceId).apply {
                setOnCompletionListener { player -> player.release() }
                start()
            }
        }.isSuccess
    }
    fun speak(message: String, utteranceId: String, fallbackResource: String): Boolean {
        val result = textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        return result == TextToSpeech.SUCCESS || playBundledReminder(fallbackResource)
    }
    LaunchedEffect(
        running,
        mode,
        preferences.remindBeforeEnd,
        preferences.reminderMinutesBeforeEnd,
        preferences.remindAtEnd,
        textToSpeech,
    ) {
        while (running) {
            val reminderSeconds = preferences.reminderMinutesBeforeEnd * 60L
            if (
                mode == TimerMode.COUNTDOWN &&
                preferences.remindBeforeEnd &&
                !spokenBeforeEnd &&
                seconds == reminderSeconds &&
                seconds > 0
            ) {
                spokenBeforeEnd = speak(
                    "距离练习结束还有${preferences.reminderMinutesBeforeEnd}分钟",
                    "before-end",
                    "reminder_${preferences.reminderMinutesBeforeEnd}",
                )
            }
            delay(1000)
            if (mode == TimerMode.COUNTDOWN) {
                if (seconds > 0) seconds--
                if (seconds == 0L) {
                    if (preferences.remindAtEnd && !spokenAtEnd) {
                        spokenAtEnd = speak("练习时间结束", "timer-end", "reminder_end")
                    }
                    running = false
                }
            } else {
                seconds++
            }
        }
    }
    val time = "%02d:%02d".format(seconds / 60, seconds % 60)
    val resetTimer = {
        running = false
        hasStarted = false
        spokenBeforeEnd = false
        spokenAtEnd = false
        seconds = if (mode == TimerMode.COUNTDOWN) minutes * 60L else 0L
    }
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(LocalPrepColors.current.primary, LocalPrepColors.current.gradientEnd)))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                .clickable {
                    if (!hasStarted) onPracticeStarted()
                    hasStarted = true
                    running = !running
                }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Text(
                when {
                    running -> "  暂停"
                    hasStarted -> "  继续"
                    else -> "  开始${if (mode == TimerMode.COUNTDOWN) "倒计时" else "计时"}"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(time, color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
        Row(
            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = .18f))
                .clickable(onClick = resetTimer)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(" 重来", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
