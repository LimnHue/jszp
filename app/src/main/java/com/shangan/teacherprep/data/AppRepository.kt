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
            addBundledTrials(SampleData.create()).also(::saveBlocking)
        } else {
            runCatching { json.decodeFromString<AppData>(storeFile.readText()) }
                .map(::migrate)
                .getOrElse { SampleData.create().also(::saveBlocking) }
        }
    }

    suspend fun save(data: AppData) = withContext(Dispatchers.IO) {
        saveBlocking(data)
    }

    suspend fun exportAll(data: AppData): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "教招上岸_完整备考库.json")
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
        val file = File(exportDir, "教招上岸_${scope.stage}_${scope.subject}_${scope.textbookVersion}.json")
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

    private fun migrate(data: AppData): AppData {
        val originalVersion = data.schemaVersion
        var migrated = data
        if (originalVersion < 2) {
            // Version 1 always had a selected scope but no onboarding completion flag.
            migrated = migrated.copy(
                preferences = migrated.preferences.copy(hasCompletedLibrarySelection = true),
            )
        }
        if (originalVersion < 5) {
            migrated = migrateTextbookVersions(migrated)
        }
        if (originalVersion < 3) {
            migrated = addBundledTrials(migrated)
        }
        if (originalVersion < 4) {
            migrated = enrichTrialUnits(migrated)
        }
        if (originalVersion < 8) {
            migrated = enrichTrialLessonOrderAndEvents(migrated)
        }
        if (originalVersion >= 8) return data
        return migrated.copy(schemaVersion = 8).also(::saveBlocking)
    }

    private fun addBundledTrials(data: AppData): AppData {
        val bundled = context.assets.list(BundledTrialCatalog.ASSET_DIRECTORY)
            .orEmpty()
            .filter { it.endsWith(".md", ignoreCase = true) }
            .flatMap { fileName ->
                val path = "${BundledTrialCatalog.ASSET_DIRECTORY}/$fileName"
                val markdown = context.assets.open(path).bufferedReader().use { it.readText() }
                BundledTrialCatalog.parse(fileName, markdown)
            }
            .distinctBy { "${it.scopeKey}::${normalizeTitle(it.title)}" }
        val existingKeys = data.trials
            .map { "${it.scopeKey}::${normalizeTitle(it.title)}" }
            .toSet()
        val unitsByTitle = bundled.associate { normalizeTitle(it.title) to it.unit }
        val enrichedExisting = data.trials.map { lesson ->
            if (lesson.unit.isNotBlank()) lesson
            else lesson.copy(unit = unitsByTitle[normalizeTitle(lesson.title)] ?: "其他")
        }
        val additions = bundled.filter {
            "${it.scopeKey}::${normalizeTitle(it.title)}" !in existingKeys
        }
        val bundledScope = LibraryScope("初中", "语文", "人教版")
        val scopeKey = bundledScope.key
        val currentConfig = data.scopeConfigs[scopeKey] ?: ScopeDefaults.create(bundledScope)
        val bundledTextbooks = bundled.map { it.textbook }
        val bundledGenres = bundled.map { it.genre }
        return data.copy(
            schemaVersion = 8,
            scopeConfigs = data.scopeConfigs + (
                scopeKey to currentConfig.copy(
                    textbooks = (currentConfig.textbooks + bundledTextbooks).distinct(),
                    units = (currentConfig.units + bundled.map { it.unit }).distinct(),
                    genres = (currentConfig.genres + bundledGenres).distinct(),
                )
            ),
            trials = enrichedExisting + additions,
        )
    }

    private fun normalizeTitle(title: String): String {
        return title.replace("《", "").replace("》", "").replace(" ", "").trim()
    }

    private fun enrichTrialUnits(data: AppData): AppData {
        val bundled = context.assets.list(BundledTrialCatalog.ASSET_DIRECTORY)
            .orEmpty()
            .filter { it.endsWith(".md", ignoreCase = true) }
            .flatMap { fileName ->
                val path = "${BundledTrialCatalog.ASSET_DIRECTORY}/$fileName"
                val markdown = context.assets.open(path).bufferedReader().use { it.readText() }
                BundledTrialCatalog.parse(fileName, markdown)
            }
        val unitsByTitle = bundled.associate { normalizeTitle(it.title) to it.unit }
        val trials = data.trials.map { lesson ->
            if (lesson.unit.isNotBlank()) lesson
            else lesson.copy(unit = unitsByTitle[normalizeTitle(lesson.title)] ?: "其他")
        }
        return data.copy(
            schemaVersion = 8,
            trials = trials,
            scopeConfigs = data.scopeConfigs.mapValues { (scopeKey, config) ->
                val scopeUnits = trials.filter { it.scopeKey == scopeKey }.map { it.unit }.filter { it.isNotBlank() }
                config.copy(units = (config.units + scopeUnits).distinct())
            },
        )
    }

    private fun migrateTextbookVersions(data: AppData): AppData {
        fun migrateKey(key: String): String {
            return if (key.split("::").size >= 3) key else "$key::人教版"
        }

        fun scopeFromKey(key: String): LibraryScope {
            val parts = migrateKey(key).split("::")
            return LibraryScope(
                stage = parts.getOrElse(0) { "初中" },
                subject = parts.getOrElse(1) { "语文" },
                textbookVersion = parts.getOrElse(2) { "人教版" },
            )
        }

        val configs = data.scopeConfigs.entries.associate { (oldKey, config) ->
            val newKey = migrateKey(oldKey)
            val scope = scopeFromKey(newKey)
            val migratedConfig = if (config == ScopeConfig()) ScopeDefaults.create(scope) else config
            newKey to migratedConfig
        }
        return data.copy(
            schemaVersion = 8,
            preferences = data.preferences.copy(
                selectedScope = data.preferences.selectedScope.copy(
                    textbookVersion = data.preferences.selectedScope.textbookVersion.ifBlank { "人教版" },
                ),
            ),
            scopeConfigs = configs,
            trials = data.trials.map { it.copy(scopeKey = migrateKey(it.scopeKey)) },
            structuredQuestions = data.structuredQuestions.map { it.copy(scopeKey = migrateKey(it.scopeKey)) },
            templates = data.templates.map { it.copy(scopeKey = migrateKey(it.scopeKey)) },
        )
    }

    private fun enrichTrialLessonOrderAndEvents(data: AppData): AppData {
        val bundled = context.assets.list(BundledTrialCatalog.ASSET_DIRECTORY)
            .orEmpty()
            .filter { it.endsWith(".md", ignoreCase = true) }
            .flatMap { fileName ->
                val path = "${BundledTrialCatalog.ASSET_DIRECTORY}/$fileName"
                val markdown = context.assets.open(path).bufferedReader().use { it.readText() }
                BundledTrialCatalog.parse(fileName, markdown)
            }
        val bundledById = bundled.associateBy { it.id }
        val bundledByTitle = bundled.associateBy { "${it.scopeKey}::${normalizeTitle(it.title)}" }
        val trials = data.trials.map { lesson ->
            val bundledLesson = bundledById[lesson.id]
                ?: bundledByTitle["${lesson.scopeKey}::${normalizeTitle(lesson.title)}"]
            if (lesson.lessonOrder > 0 || bundledLesson == null) lesson
            else lesson.copy(
                lessonOrder = bundledLesson.lessonOrder,
            )
        }
        val events = data.practiceEvents.ifEmpty {
            buildList {
                trials.filter { it.lastPracticedAt != null }.forEach {
                    add(PracticeEvent(scopeKey = it.scopeKey, module = PracticeModule.TRIAL, itemId = it.id, title = it.title, practicedAt = it.lastPracticedAt!!))
                }
                data.structuredQuestions.filter { it.lastPracticedAt != null }.forEach {
                    add(PracticeEvent(scopeKey = it.scopeKey, module = PracticeModule.STRUCTURED, itemId = it.id, title = it.question, practicedAt = it.lastPracticedAt!!))
                }
                data.templates.filter { it.lastPracticedAt != null }.forEach {
                    add(PracticeEvent(scopeKey = it.scopeKey, module = PracticeModule.TEMPLATE, itemId = it.id, title = it.name, practicedAt = it.lastPracticedAt!!))
                }
            }
        }
        return data.copy(trials = trials, practiceEvents = events)
    }
}
