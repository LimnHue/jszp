package com.shangan.teacherprep.feature

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.shangan.teacherprep.data.PracticeMedia
import com.shangan.teacherprep.data.PracticeMediaType
import com.shangan.teacherprep.data.TrialLesson
import com.shangan.teacherprep.ui.RoundedCard
import com.shangan.teacherprep.ui.theme.LocalPrepColors
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val mediaTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss")

@Composable
fun TrialMediaSection(
    lesson: TrialLesson,
    onMediaSaved: (PracticeMediaType, String) -> Unit,
    onMediaDelete: (String) -> Unit,
    onMediaRename: (String, String) -> Unit,
) = PracticeMediaSection(
    itemId = lesson.id,
    mediaItems = lesson.practiceMedia,
    practiceLabel = "试讲",
    onMediaSaved = onMediaSaved,
    onMediaDelete = onMediaDelete,
    onMediaRename = onMediaRename,
)

@Composable
fun PracticeMediaSection(
    itemId: String,
    mediaItems: List<PracticeMedia>,
    practiceLabel: String,
    onMediaSaved: (PracticeMediaType, String) -> Unit,
    onMediaDelete: (String) -> Unit,
    onMediaRename: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var pendingVideo by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var pendingDelete by remember { mutableStateOf<PracticeMedia?>(null) }
    var pendingActions by remember { mutableStateOf<PracticeMedia?>(null) }
    var pendingRename by remember { mutableStateOf<PracticeMedia?>(null) }
    var renameText by remember { mutableStateOf("") }

    fun nextAttemptNumber(type: PracticeMediaType): Int {
        val existing = mediaItems.filter { it.type == type }
        return maxOf(existing.size, existing.maxOfOrNull { it.attemptNumber } ?: 0) + 1
    }

    fun mediaFile(type: PracticeMediaType, extension: String): File {
        val directory = File(context.filesDir, "practice_media/$itemId").apply { mkdirs() }
        val mediaLabel = if (type == PracticeMediaType.VIDEO) "视频" else "录音"
        return File(directory, "第${nextAttemptNumber(type)}次${practiceLabel}${mediaLabel}_${System.currentTimeMillis()}.$extension")
    }

    fun startRecording() {
        val output = mediaFile(PracticeMediaType.AUDIO, "m4a")
        val nextRecorder = createRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }
        recordingFile = output
        recorder = nextRecorder
    }

    fun stopRecording(save: Boolean) {
        val active = recorder ?: return
        runCatching { active.stop() }
        active.release()
        recorder = null
        val output = recordingFile
        recordingFile = null
        if (save && output != null && output.exists() && output.length() > 0) {
            onMediaSaved(PracticeMediaType.AUDIO, output.absolutePath)
        } else {
            output?.delete()
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { saved ->
        val file = pendingVideo
        pendingVideo = null
        if (saved && file != null && file.exists() && file.length() > 0) {
            onMediaSaved(PracticeMediaType.VIDEO, file.absolutePath)
        } else {
            file?.delete()
        }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCatching(::startRecording)
    }

    DisposableEffect(Unit) {
        onDispose { stopRecording(save = false) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RoundedCard(containerColor = Color(0xFFFCFBF8)) {
            Text("${practiceLabel}影像与录音", fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(
                "每次拍摄或录音都会保存在当前课程下，并自动记录日期时间。",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MediaAction(
                    text = "拍摄视频",
                    icon = Icons.Rounded.Videocam,
                    modifier = Modifier.weight(1f),
                ) {
                    val output = mediaFile(PracticeMediaType.VIDEO, "mp4")
                    pendingVideo = output
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", output)
                    videoLauncher.launch(uri)
                }
                MediaAction(
                    text = if (recorder == null) "开始录音" else "停止录音",
                    icon = if (recorder == null) Icons.Rounded.Mic else Icons.Rounded.Stop,
                    modifier = Modifier.weight(1f),
                    active = recorder != null,
                ) {
                    if (recorder != null) {
                        stopRecording(save = true)
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        runCatching(::startRecording)
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }

        if (mediaItems.isEmpty()) {
            RoundedCard(containerColor = Color(0xFFF8F9FC)) {
                Text("暂无视频或录音", color = Color.Gray)
            }
        } else {
            mediaItems.sortedByDescending { it.createdAt }.forEach { media ->
                MediaItem(
                    media = media,
                    practiceLabel = practiceLabel,
                    fallbackAttemptNumber = fallbackAttemptNumber(media, mediaItems),
                    onOpen = { openMedia(context, media) },
                    onDeleteRequest = { pendingDelete = media },
                    onLongPress = { pendingActions = media },
                )
            }
        }
    }

    pendingDelete?.let { media ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录？") },
            text = { Text("${mediaDisplayName(media, practiceLabel, fallbackAttemptNumber(media, mediaItems))}及其本地文件将被永久删除。") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDelete = null
                        onMediaDelete(media.id)
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }

    pendingActions?.let { media ->
        AlertDialog(
            onDismissRequest = { pendingActions = null },
            title = { Text(mediaDisplayName(media, practiceLabel, fallbackAttemptNumber(media, mediaItems))) },
            text = { Text("选择要执行的操作") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingActions = null
                        renameText = mediaDisplayName(media, practiceLabel, fallbackAttemptNumber(media, mediaItems))
                        pendingRename = media
                    },
                ) {
                    Icon(Icons.Rounded.Edit, null)
                    Text(" 重命名")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingActions = null
                        pendingDelete = media
                    },
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE55245))
                    Text(" 删除", color = Color(0xFFE55245))
                }
            },
        )
    }

    pendingRename?.let { media ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text("重命名记录") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(50) },
                    label = { Text("名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMediaRename(media.id, renameText)
                        pendingRename = null
                    },
                    enabled = renameText.isNotBlank(),
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MediaItem(
    media: PracticeMedia,
    practiceLabel: String,
    fallbackAttemptNumber: Int,
    onOpen: () -> Unit,
    onDeleteRequest: () -> Unit,
    onLongPress: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
            false
        },
        positionalThreshold = { distance -> distance * .3f },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                Modifier.fillMaxWidth()
                    .background(Color(0xFFE55245), RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("左滑删除", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.size(8.dp))
                    Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Color.White)
                }
            }
        },
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF1F4F7),
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (media.type == PracticeMediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = LocalPrepColors.current.primary,
                    modifier = Modifier.size(30.dp),
                )
                Column(Modifier.padding(start = 13.dp).weight(1f)) {
                    Text(mediaDisplayName(media, practiceLabel, fallbackAttemptNumber), fontWeight = FontWeight.Bold)
                    Text(formatMediaTime(media.createdAt), color = Color.Gray, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.PlayCircle, contentDescription = "播放", tint = LocalPrepColors.current.primary)
            }
        }
    }
}

private fun fallbackAttemptNumber(media: PracticeMedia, allMedia: List<PracticeMedia>): Int {
    return allMedia
        .filter { it.type == media.type }
        .sortedWith(compareBy<PracticeMedia> { it.createdAt }.thenBy { it.id })
        .indexOfFirst { it.id == media.id }
        .plus(1)
        .coerceAtLeast(1)
}

private fun mediaDisplayName(media: PracticeMedia, practiceLabel: String, fallbackAttemptNumber: Int): String {
    media.displayName?.takeIf { it.isNotBlank() }?.let { return it }
    val attemptNumber = media.attemptNumber.takeIf { it > 0 } ?: fallbackAttemptNumber
    val mediaLabel = if (media.type == PracticeMediaType.VIDEO) "视频" else "录音"
    return "第${attemptNumber}次${practiceLabel}$mediaLabel"
}

@Composable
private fun MediaAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (active) Color(0xFFE9EEF2) else Color(0xFFFCFBF8),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = LocalPrepColors.current.primary)
            Spacer(Modifier.size(7.dp))
            Text(text, color = LocalPrepColors.current.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Suppress("DEPRECATION")
private fun createRecorder(context: android.content.Context): MediaRecorder {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
}

private fun formatMediaTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(mediaTimeFormatter)
}

private fun openMedia(context: android.content.Context, media: PracticeMedia) {
    val file = File(media.filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val mime = if (media.type == PracticeMediaType.VIDEO) "video/mp4" else "audio/mp4"
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }
}
