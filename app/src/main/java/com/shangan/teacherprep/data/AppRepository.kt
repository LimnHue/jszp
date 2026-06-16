package com.shangan.teacherprep.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class AppRepository(private val context: Context) {
    private val storeFile = File(context.filesDir, "teacher_prep_library.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(): AppData = withContext(Dispatchers.IO) {
        if (!storeFile.exists()) {
            addBundledStructured(addBundledTrials(SampleData.create())).also(::saveBlocking)
        } else {
            runCatching { json.decodeFromString<AppData>(storeFile.readText()) }
                .map(::migrate)
                .getOrElse { addBundledStructured(addBundledTrials(SampleData.create())).also(::saveBlocking) }
        }
    }

    suspend fun save(data: AppData) = withContext(Dispatchers.IO) {
        saveBlocking(data)
    }

    suspend fun exportAll(data: AppData): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "教招记录_完整备考库.json")
        file.writeText(json.encodeToString(data))
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun exportScope(data: AppData, scope: LibraryScope): Uri = withContext(Dispatchers.IO) {
        val scoped = data.copy(
            preferences = data.preferences.copy(selectedScope = scope),
            scopeConfigs = data.scopeConfigs.filterKeys { it == scope.key },
            trials = data.trials.filter { it.scopeKey == scope.key },
            structuredQuestions = data.structuredQuestions.filter { it.scopeKey == SharedLibrary.key },
            templates = data.templates.filter { it.scopeKey == SharedLibrary.key },
            practiceEvents = data.practiceEvents.filter {
                it.scopeKey == scope.key || it.scopeKey == SharedLibrary.key
            },
        )
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "教招记录_${scope.stage}_${scope.subject}_${scope.textbookVersion}.json")
        file.writeText(json.encodeToString(scoped))
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun exportTrialMarkdown(lesson: TrialLesson): Uri = withContext(Dispatchers.IO) {
        val markdown = buildString {
            appendLine("# ${lesson.title}")
            appendLine()
            appendLine("- 教材：${lesson.textbook}")
            if (lesson.unit.isNotBlank()) appendLine("- 单元：${lesson.unit}")
            if (lesson.lessonOrder > 0) appendLine("- 课次：第${lesson.lessonOrder}课")
            appendLine("- 题材：${lesson.genre}")
            appendLine("- 试讲时长：${lesson.durationMinutes}分钟")
            appendLine()
            appendLine("## 课程信息")
            appendLine()
            appendLine(lesson.courseInfoMarkdown)
            lesson.bodySections.forEach { section ->
                appendLine()
                appendLine("## ${section.title}")
                appendLine()
                appendLine(section.markdown)
            }
            lesson.boardImageUri?.let {
                appendLine()
                appendLine("## 板书设计")
                appendLine()
                appendLine("板书图片：$it")
            }
        }
        exportMarkdown(lesson.title, markdown)
    }

    suspend fun exportStructuredMarkdown(question: StructuredQuestion): Uri = withContext(Dispatchers.IO) {
        val markdown = buildString {
            appendLine("# ${question.question}")
            appendLine()
            appendLine("- 类型：${question.category}")
            appendLine("- 重要程度：${question.importance}星")
            appendLine("- 建议时长：${question.durationMinutes}分钟")
            question.answerSections.forEach { section ->
                appendLine()
                appendLine("## ${section.title}")
                appendLine()
                appendLine(section.markdown)
            }
        }
        exportMarkdown(question.question, markdown)
    }

    suspend fun exportTemplateMarkdown(template: AnswerTemplate): Uri = withContext(Dispatchers.IO) {
        val markdown = buildString {
            appendLine("# ${template.name}")
            appendLine()
            appendLine("- 类型：${template.category}")
            appendLine("- 模块：${template.module}")
            appendLine()
            appendLine(template.contentMarkdown)
        }
        exportMarkdown(template.name, markdown)
    }

    suspend fun exportTrialMarkdown(lessons: List<TrialLesson>): Uri = withContext(Dispatchers.IO) {
        exportMarkdown("批量试讲_${lessons.size}篇", lessons.joinToString("\n\n---\n\n") { buildTrialMarkdown(it) })
    }

    suspend fun exportStructuredMarkdown(questions: List<StructuredQuestion>): Uri = withContext(Dispatchers.IO) {
        exportMarkdown("批量结构化_${questions.size}题", questions.joinToString("\n\n---\n\n") { buildStructuredMarkdown(it) })
    }

    suspend fun exportTemplateMarkdown(templates: List<AnswerTemplate>): Uri = withContext(Dispatchers.IO) {
        exportMarkdown("批量模板_${templates.size}篇", templates.joinToString("\n\n---\n\n") { buildTemplateMarkdown(it) })
    }

    suspend fun importBackup(uri: Uri, current: AppData): AppData = withContext(Dispatchers.IO) {
        val incoming = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            json.decodeFromString<AppData>(reader.readText())
        } ?: error("无法读取备考库文件")
        val importData = normalizeSharedLibraryItems(remapSingleScopeImportIfNeeded(incoming, current))

        // Imported libraries are merged by stable IDs so sharing never erases local notes.
        current.copy(
            scopeConfigs = current.scopeConfigs + importData.scopeConfigs.filterKeys { it != SharedLibrary.key },
            trials = (current.trials + importData.trials).distinctBy { it.id },
            structuredQuestions = (current.structuredQuestions + importData.structuredQuestions).distinctBy { it.id },
            templates = (current.templates + importData.templates).distinctBy { it.id },
        )
    }

    private fun remapSingleScopeImportIfNeeded(incoming: AppData, current: AppData): AppData {
        val incomingScopeKeys = (
            incoming.scopeConfigs.keys +
                incoming.trials.map { it.scopeKey } +
                incoming.structuredQuestions.map { it.scopeKey } +
                incoming.templates.map { it.scopeKey }
            )
            .filter { it.isNotBlank() }
            .filter { it != SharedLibrary.key }
            .distinct()
        if (incomingScopeKeys.size != 1) return incoming

        val sourceScopeKey = incomingScopeKeys.single()
        val targetScope = current.preferences.selectedScope
        val targetScopeKey = targetScope.key
        if (sourceScopeKey == targetScopeKey) return incoming

        val currentConfig = current.scopeConfigs[targetScopeKey] ?: ScopeDefaults.create(targetScope)
        val incomingConfig = incoming.scopeConfigs[sourceScopeKey] ?: ScopeConfig()
        return incoming.copy(
            preferences = incoming.preferences.copy(selectedScope = targetScope),
            scopeConfigs = mapOf(targetScopeKey to mergeScopeConfig(currentConfig, incomingConfig)),
            trials = incoming.trials.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    scopeKey = targetScopeKey,
                    practiceMedia = emptyList(),
                    practiceCount = 0,
                    lastPracticedAt = null,
                )
            },
            structuredQuestions = incoming.structuredQuestions.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    scopeKey = SharedLibrary.key,
                    practiceMedia = emptyList(),
                    practiceCount = 0,
                    lastPracticedAt = null,
                )
            },
            templates = incoming.templates.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    scopeKey = SharedLibrary.key,
                    practiceMedia = emptyList(),
                    practiceCount = 0,
                    lastPracticedAt = null,
                )
            },
            practiceEvents = emptyList(),
        )
    }

    private fun normalizeSharedLibraryItems(data: AppData): AppData {
        return data.copy(
            scopeConfigs = data.scopeConfigs.filterKeys { it != SharedLibrary.key },
            structuredQuestions = data.structuredQuestions.map { it.copy(scopeKey = SharedLibrary.key) },
            templates = data.templates.map { it.copy(scopeKey = SharedLibrary.key) },
            practiceEvents = data.practiceEvents.map { event ->
                when (event.module) {
                    PracticeModule.STRUCTURED,
                    PracticeModule.TEMPLATE,
                    -> event.copy(scopeKey = SharedLibrary.key)
                    PracticeModule.TRIAL -> event
                }
            },
        )
    }

    private fun mergeScopeConfig(current: ScopeConfig, incoming: ScopeConfig): ScopeConfig {
        fun mergeValues(a: List<String>, b: List<String>): List<String> {
            return (a + b).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        }
        return current.copy(
            textbooks = mergeValues(current.textbooks, incoming.textbooks),
            units = mergeValues(current.units, incoming.units),
            genres = mergeValues(current.genres, incoming.genres),
            structuredTypes = mergeValues(current.structuredTypes, incoming.structuredTypes),
            templateTypes = mergeValues(current.templateTypes, incoming.templateTypes),
            trialSectionNames = mergeValues(current.trialSectionNames, incoming.trialSectionNames),
            structuredSectionNames = mergeValues(current.structuredSectionNames, incoming.structuredSectionNames),
        )
    }

    private fun saveBlocking(data: AppData) {
        val temp = File(storeFile.parentFile, "${storeFile.name}.tmp")
        temp.writeText(json.encodeToString(data))
        if (storeFile.exists()) storeFile.delete()
        temp.renameTo(storeFile)
    }

    private fun exportMarkdown(title: String, markdown: String): Uri {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeTitle = title.replace(Regex("""[\\/:*?"<>|]"""), "_").take(60).ifBlank { "导出内容" }
        val file = File(exportDir, "$safeTitle.md")
        file.writeText(markdown)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun buildTrialMarkdown(lesson: TrialLesson): String = buildString {
        appendLine("# ${lesson.title}")
        appendLine()
        appendLine("- 教材：${lesson.textbook}")
        if (lesson.unit.isNotBlank()) appendLine("- 单元：${lesson.unit}")
        if (lesson.lessonOrder > 0) appendLine("- 课次：第${lesson.lessonOrder}课")
        appendLine("- 题材：${lesson.genre}")
        appendLine("- 试讲时长：${lesson.durationMinutes}分钟")
        appendLine()
        appendLine("## 课程信息")
        appendLine()
        appendLine(lesson.courseInfoMarkdown)
        lesson.bodySections.forEach { section ->
            appendLine()
            appendLine("## ${section.title}")
            appendLine()
            appendLine(section.markdown)
        }
        lesson.boardImageUri?.let {
            appendLine()
            appendLine("## 板书设计")
            appendLine()
            appendLine("板书图片：$it")
        }
    }

    private fun buildStructuredMarkdown(question: StructuredQuestion): String = buildString {
        appendLine("# ${question.question}")
        appendLine()
        appendLine("- 类型：${question.category}")
        appendLine("- 重要程度：${question.importance}星")
        appendLine("- 建议时长：${question.durationMinutes}分钟")
        question.answerSections.forEach { section ->
            appendLine()
            appendLine("## ${section.title}")
            appendLine()
            appendLine(section.markdown)
        }
    }

    private fun buildTemplateMarkdown(template: AnswerTemplate): String = buildString {
        appendLine("# ${template.name}")
        appendLine()
        appendLine("- 类型：${template.category}")
        if (template.module.isNotBlank()) appendLine("- 模块：${template.module}")
        appendLine()
        appendLine(template.contentMarkdown)
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
        if (originalVersion < 9) {
            migrated = numberPracticeMedia(migrated)
        }
        if (originalVersion < 14) {
            migrated = migrated.copy(
                preferences = migrated.preferences.copy(
                    uiScale = 1f,
                    fontScale = 1f,
                ),
            )
        }
        if (originalVersion < 15) {
            migrated = normalizeSharedLibraryItems(migrated)
        }
        if (originalVersion < 16) {
            migrated = addBundledStructured(migrated)
        }
        if (originalVersion < 17) {
            migrated = compactDefaultAppearance(migrated)
        }
        if (originalVersion >= 17) return data
        return migrated.copy(schemaVersion = 17).also(::saveBlocking)
    }

    private fun compactDefaultAppearance(data: AppData): AppData {
        val preferences = data.preferences
        return data.copy(
            schemaVersion = 17,
            preferences = preferences.copy(
                logoScale = if (preferences.logoScale == 1f) 0.86f else preferences.logoScale,
                uiScale = if (preferences.uiScale == 1f) 0.92f else preferences.uiScale,
                fontScale = if (preferences.fontScale == 1f) 0.93f else preferences.fontScale,
            ),
        )
    }

    private fun addBundledStructured(data: AppData): AppData {
        val assetDirectory = "structured_questions"
        val bundled = context.assets.list(assetDirectory)
            .orEmpty()
            .filter { it.endsWith(".json", ignoreCase = true) }
            .flatMap { fileName ->
                val path = "$assetDirectory/$fileName"
                val content = context.assets.open(path).bufferedReader().use { it.readText() }
                json.decodeFromString<List<BundledStructuredQuestion>>(content)
            }
        if (bundled.isEmpty()) return data.copy(schemaVersion = 17)

        val existingIds = data.structuredQuestions.map { it.id }.toSet()
        val existingQuestions = data.structuredQuestions.map { normalizeTitle(it.question) }.toSet()
        val additions = bundled
            .filter { it.id !in existingIds && normalizeTitle(it.question) !in existingQuestions }
            .map { item ->
                StructuredQuestion(
                    id = item.id,
                    scopeKey = SharedLibrary.key,
                    category = item.category,
                    question = item.question,
                    answerSections = listOf(
                        ContentSection(
                            id = "${item.id}_answer",
                            title = "逐字稿参考答案",
                            markdown = item.answerMarkdown,
                        ),
                    ),
                    durationMinutes = data.preferences.defaultStructuredMinutes,
                    importance = item.importance.coerceIn(1, 5),
                )
            }
        val bundledCategories = bundled.map { it.category }.filter { it.isNotBlank() }
        return data.copy(
            schemaVersion = 17,
            scopeConfigs = data.scopeConfigs.mapValues { (_, config) ->
                config.copy(structuredTypes = (config.structuredTypes + bundledCategories).distinct())
            },
            structuredQuestions = data.structuredQuestions + additions,
        )
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
            schemaVersion = 17,
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
            schemaVersion = 17,
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
            schemaVersion = 17,
            preferences = data.preferences.copy(
                selectedScope = data.preferences.selectedScope.copy(
                    textbookVersion = data.preferences.selectedScope.textbookVersion.ifBlank { "人教版" },
                ),
            ),
            scopeConfigs = configs,
            trials = data.trials.map { it.copy(scopeKey = migrateKey(it.scopeKey)) },
            structuredQuestions = data.structuredQuestions.map { it.copy(scopeKey = SharedLibrary.key) },
            templates = data.templates.map { it.copy(scopeKey = SharedLibrary.key) },
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

    private fun numberPracticeMedia(data: AppData): AppData {
        return data.copy(
            trials = data.trials.map { lesson ->
                val numbersById = lesson.practiceMedia
                    .groupBy { it.type }
                    .values
                    .flatMap { mediaOfType ->
                        mediaOfType
                            .sortedWith(compareBy<PracticeMedia> { it.createdAt }.thenBy { it.id })
                            .mapIndexed { index, media -> media.id to (index + 1) }
                    }
                    .toMap()
                lesson.copy(
                    practiceMedia = lesson.practiceMedia.map { media ->
                        if (media.attemptNumber > 0) media
                        else media.copy(attemptNumber = numbersById.getValue(media.id))
                    },
                )
            },
        )
    }
}

@Serializable
private data class BundledStructuredQuestion(
    val id: String,
    val category: String,
    val question: String,
    val answerMarkdown: String,
    val importance: Int = 3,
)
