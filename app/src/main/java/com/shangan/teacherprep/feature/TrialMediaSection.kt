package com.shangan.teacherprep.feature

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
) {
    val context = LocalContext.current
    var pendingVideo by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    fun mediaFile(extension: String): File {
        val directory = File(context.filesDir, "practice_media/${lesson.id}").apply { mkdirs() }
        return File(directory, "${System.currentTimeMillis()}.$extension")
    }

    fun startRecording() {
        val output = mediaFile("m4a")
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
        RoundedCard(containerColor = Color(0xFFF1F7FF)) {
            Text("试讲影像与录音", fontSize = 21.sp, fontWeight = FontWeight.Black)
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
                    val output = mediaFile("mp4")
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

        if (lesson.practiceMedia.isEmpty()) {
            RoundedCard(containerColor = Color(0xFFF8F9FC)) {
                Text("暂无视频或录音", color = Color.Gray)
            }
        } else {
            lesson.practiceMedia.sortedByDescending { it.createdAt }.forEach { media ->
                Surface(
                    onClick = { openMedia(context, media) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (media.type == PracticeMediaType.VIDEO) Color(0xFFF2F7FF) else Color(0xFFFFF5F1),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (media.type == PracticeMediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = LocalPrepColors.current.primary,
                            modifier = Modifier.size(30.dp),
                        )
                        Column(Modifier.padding(start = 13.dp).weight(1f)) {
                            Text(
                                if (media.type == PracticeMediaType.VIDEO) "试讲视频" else "试讲录音",
                                fontWeight = FontWeight.Bold,
                            )
                            Text(formatMediaTime(media.createdAt), color = Color.Gray, fontSize = 13.sp)
                        }
                        Icon(Icons.Rounded.PlayCircle, contentDescription = "播放", tint = LocalPrepColors.current.primary)
                    }
                }
            }
        }
    }
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
        color = if (active) Color(0xFFFFE3DD) else Color.White,
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
