package com.shangan.teacherprep.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AppRepository(private val context: Context) {
    private val storeFile = File(context.filesDir, "teacher_prep_library.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(): AppData = withContext(Dispatchers.IO) {
        if (!storeFile.exists()) {
            SampleData.create().also(::saveBlocking)
        } else {
            runCatching { json.decodeFromString<AppData>(storeFile.readText()) }
                .getOrElse { SampleData.create().also(::saveBlocking) }
        }
    }

    suspend fun save(data: AppData) = withContext(Dispatchers.IO) {
        saveBlocking(data)
    }

    suspend fun exportAll(data: AppData): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "上岸备课_完整备考库.json")
        file.writeText(json.encodeToString(data))
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun exportScope(data: AppData, scope: LibraryScope): Uri = withContext(Dispatchers.IO) {
        val scoped = data.copy(
            preferences = data.preferences.copy(selectedScope = scope),
            scopeConfigs = data.scopeConfigs.filterKeys { it == scope.key },
            trials = data.trials.filter { it.scopeKey == scope.key },
            structuredQuestions = data.structuredQuestions.filter { it.scopeKey == scope.key },
            templates = data.templates.filter { it.scopeKey == scope.key },
        )
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "上岸备课_${scope.stage}_${scope.subject}.json")
        file.writeText(json.encodeToString(scoped))
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun importBackup(uri: Uri, current: AppData): AppData = withContext(Dispatchers.IO) {
        val incoming = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            json.decodeFromString<AppData>(reader.readText())
        } ?: error("无法读取备考库文件")

        // Imported libraries are merged by stable IDs so sharing never erases local notes.
        current.copy(
            scopeConfigs = current.scopeConfigs + incoming.scopeConfigs,
            trials = (current.trials + incoming.trials).distinctBy { it.id },
            structuredQuestions = (current.structuredQuestions + incoming.structuredQuestions).distinctBy { it.id },
            templates = (current.templates + incoming.templates).distinctBy { it.id },
        )
    }

    private fun saveBlocking(data: AppData) {
        val temp = File(storeFile.parentFile, "${storeFile.name}.tmp")
        temp.writeText(json.encodeToString(data))
        if (storeFile.exists()) storeFile.delete()
        temp.renameTo(storeFile)
    }
}
