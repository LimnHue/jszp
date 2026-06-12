package com.shangan.teacherprep

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shangan.teacherprep.data.AnswerTemplate
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.AppRepository
import com.shangan.teacherprep.data.ContentSection
import com.shangan.teacherprep.data.FilterVisibility
import com.shangan.teacherprep.data.LibraryScope
import com.shangan.teacherprep.data.PaletteStyle
import com.shangan.teacherprep.data.PracticeEvent
import com.shangan.teacherprep.data.PracticeMedia
import com.shangan.teacherprep.data.PracticeMediaType
import com.shangan.teacherprep.data.PracticeModule
import com.shangan.teacherprep.data.ScopeConfig
import com.shangan.teacherprep.data.ScopeDefaults
import com.shangan.teacherprep.data.StructuredQuestion
import com.shangan.teacherprep.data.TimerMode
import com.shangan.teacherprep.data.TrialLesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val loading: Boolean = true,
    val data: AppData = AppData(),
    val message: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = AppUiState(loading = false, data = repository.load())
        }
    }

    fun selectScope(scope: LibraryScope) = update {
        val config = it.scopeConfigs[scope.key] ?: ScopeDefaults.create(scope)
        it.copy(
            preferences = it.preferences.copy(
                selectedScope = scope,
                hasCompletedLibrarySelection = true,
            ),
            scopeConfigs = it.scopeConfigs + (scope.key to config),
        )
    }

    fun updatePreferences(
        timerMode: TimerMode? = null,
        trialMinutes: Int? = null,
        structuredMinutes: Int? = null,
        palette: PaletteStyle? = null,
        opacity: Float? = null,
    ) = update { data ->
        data.copy(
            preferences = data.preferences.copy(
                timerMode = timerMode ?: data.preferences.timerMode,
                defaultTrialMinutes = trialMinutes ?: data.preferences.defaultTrialMinutes,
                defaultStructuredMinutes = structuredMinutes ?: data.preferences.defaultStructuredMinutes,
                palette = palette ?: data.preferences.palette,
                surfaceOpacity = opacity ?: data.preferences.surfaceOpacity,
            ),
        )
    }

    fun updateReminderPreferences(
        remindBeforeEnd: Boolean? = null,
        reminderMinutesBeforeEnd: Int? = null,
        remindAtEnd: Boolean? = null,
    ) = update { data ->
        data.copy(
            preferences = data.preferences.copy(
                remindBeforeEnd = remindBeforeEnd ?: data.preferences.remindBeforeEnd,
                reminderMinutesBeforeEnd = reminderMinutesBeforeEnd
                    ?.coerceIn(1, 30)
                    ?: data.preferences.reminderMinutesBeforeEnd,
                remindAtEnd = remindAtEnd ?: data.preferences.remindAtEnd,
            ),
        )
    }

    fun updateFilterVisibility(transform: (FilterVisibility) -> FilterVisibility) = update { data ->
        data.copy(
            preferences = data.preferences.copy(
                filterVisibility = transform(data.preferences.filterVisibility),
            ),
        )
    }

    fun addStage(value: String) = update { data ->
        if (value.isBlank()) data else data.copy(preferences = data.preferences.copy(stages = (data.preferences.stages + value.trim()).distinct()))
    }

    fun addSubject(value: String) = update { data ->
        if (value.isBlank()) data else data.copy(preferences = data.preferences.copy(subjects = (data.preferences.subjects + value.trim()).distinct()))
    }

    fun addTextbookVersion(value: String) = update { data ->
        if (value.isBlank()) data
        else data.copy(
            preferences = data.preferences.copy(
                textbookVersions = (data.preferences.textbookVersions + value.trim()).distinct(),
            ),
        )
    }

    fun updateScopeConfig(transform: (ScopeConfig) -> ScopeConfig) = update { data ->
        val key = data.preferences.selectedScope.key
        val config = transform(data.scopeConfigs[key] ?: ScopeDefaults.create(data.preferences.selectedScope))
        data.copy(scopeConfigs = data.scopeConfigs + (key to config))
    }

    fun addTrial(
        title: String,
        textbook: String,
        unit: String,
        lessonOrder: Int,
        genre: String,
        markdown: String,
        sections: List<ContentSection>,
        boardImageUri: String?,
        importance: Int,
    ) = update { data ->
        val scope = data.preferences.selectedScope
        val item = TrialLesson(
            scopeKey = scope.key,
            title = title.ifBlank { "未命名试讲" },
            textbook = textbook,
            unit = unit,
            lessonOrder = lessonOrder.coerceAtLeast(0),
            genre = genre,
            courseInfoMarkdown = markdown,
            bodySections = sections,
            boardImageUri = boardImageUri,
            durationMinutes = data.preferences.defaultTrialMinutes,
            importance = importance.coerceIn(1, 5),
        )
        data.copy(trials = listOf(item) + data.trials)
    }

    fun updateTrial(
        id: String,
        title: String,
        textbook: String,
        unit: String,
        lessonOrder: Int,
        genre: String,
        markdown: String,
        sections: List<ContentSection>,
        boardImageUri: String?,
        importance: Int,
    ) = update { data ->
        data.copy(
            trials = data.trials.map { item ->
                if (item.id != id) item else item.copy(
                    title = title.ifBlank { "未命名试讲" },
                    textbook = textbook,
                    unit = unit,
                    lessonOrder = lessonOrder.coerceAtLeast(0),
                    genre = genre,
                    courseInfoMarkdown = markdown,
                    bodySections = sections,
                    boardImageUri = boardImageUri,
                    importance = importance.coerceIn(1, 5),
                    updatedAt = System.currentTimeMillis(),
                )
            },
        )
    }

    fun addStructured(category: String, question: String, sections: List<ContentSection>, importance: Int) = update { data ->
        val item = StructuredQuestion(
            scopeKey = data.preferences.selectedScope.key,
            category = category,
            question = question.ifBlank { "未命名结构化问题" },
            answerSections = sections,
            durationMinutes = data.preferences.defaultStructuredMinutes,
            importance = importance.coerceIn(1, 5),
        )
        data.copy(structuredQuestions = listOf(item) + data.structuredQuestions)
    }

    fun updateStructured(id: String, category: String, question: String, sections: List<ContentSection>, importance: Int) = update { data ->
        data.copy(
            structuredQuestions = data.structuredQuestions.map { item ->
                if (item.id != id) item else item.copy(
                    category = category,
                    question = question.ifBlank { "未命名结构化问题" },
                    answerSections = sections,
                    importance = importance.coerceIn(1, 5),
                    updatedAt = System.currentTimeMillis(),
                )
            },
        )
    }

    fun addTemplate(category: String, name: String, markdown: String) = update { data ->
        val item = AnswerTemplate(
            scopeKey = data.preferences.selectedScope.key,
            category = category,
            name = name.ifBlank { "未命名模板" },
            summary = markdown.lineSequence().firstOrNull().orEmpty().take(30),
            contentMarkdown = markdown,
        )
        data.copy(templates = listOf(item) + data.templates)
    }

    fun updateTemplate(id: String, category: String, name: String, markdown: String) = update { data ->
        data.copy(
            templates = data.templates.map { item ->
                if (item.id != id) item else item.copy(
                    category = category,
                    name = name.ifBlank { "未命名模板" },
                    summary = markdown.lineSequence().firstOrNull().orEmpty().take(30),
                    contentMarkdown = markdown,
                    updatedAt = System.currentTimeMillis(),
                )
            },
        )
    }

    fun toggleTrialFavorite(id: String) = update { data ->
        data.copy(trials = data.trials.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    }

    fun toggleStructuredFavorite(id: String) = update { data ->
        data.copy(structuredQuestions = data.structuredQuestions.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    }

    fun toggleTemplateFavorite(id: String) = update { data ->
        data.copy(templates = data.templates.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    }

    fun recordTrialPractice(id: String) = update { data ->
        val practicedAt = System.currentTimeMillis()
        val lesson = data.trials.firstOrNull { it.id == id } ?: return@update data
        data.copy(
            trials = data.trials.map {
                if (it.id == id) it.copy(
                    practiceCount = it.practiceCount + 1,
                    lastPracticedAt = practicedAt,
                ) else it
            },
            practiceEvents = data.practiceEvents + PracticeEvent(
                scopeKey = lesson.scopeKey,
                module = PracticeModule.TRIAL,
                itemId = lesson.id,
                title = lesson.title,
                practicedAt = practicedAt,
            ),
        )
    }

    fun recordStructuredPractice(id: String) = update { data ->
        val practicedAt = System.currentTimeMillis()
        val question = data.structuredQuestions.firstOrNull { it.id == id } ?: return@update data
        data.copy(
            structuredQuestions = data.structuredQuestions.map {
                if (it.id == id) it.copy(
                    practiceCount = it.practiceCount + 1,
                    lastPracticedAt = practicedAt,
                ) else it
            },
            practiceEvents = data.practiceEvents + PracticeEvent(
                scopeKey = question.scopeKey,
                module = PracticeModule.STRUCTURED,
                itemId = question.id,
                title = question.question,
                practicedAt = practicedAt,
            ),
        )
    }

    fun recordTemplatePractice(id: String) = update { data ->
        val practicedAt = System.currentTimeMillis()
        val template = data.templates.firstOrNull { it.id == id } ?: return@update data
        data.copy(
            templates = data.templates.map {
                if (it.id == id) it.copy(
                    practiceCount = it.practiceCount + 1,
                    lastPracticedAt = practicedAt,
                ) else it
            },
            practiceEvents = data.practiceEvents + PracticeEvent(
                scopeKey = template.scopeKey,
                module = PracticeModule.TEMPLATE,
                itemId = template.id,
                title = template.name,
                practicedAt = practicedAt,
            ),
        )
    }

    fun addTrialMedia(id: String, type: PracticeMediaType, filePath: String) = update { data ->
        val media = PracticeMedia(type = type, filePath = filePath)
        data.copy(
            trials = data.trials.map {
                if (it.id == id) it.copy(practiceMedia = it.practiceMedia + media) else it
            },
        )
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                repository.importBackup(uri, _uiState.value.data)
            }.onSuccess { imported ->
                repository.save(imported)
                _uiState.value = AppUiState(loading = false, data = imported, message = "备考库导入成功")
            }.onFailure {
                _uiState.value = _uiState.value.copy(message = "导入失败：${it.message}")
            }
        }
    }

    fun exportLibrary(allScopes: Boolean, onReady: (Uri) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val data = _uiState.value.data
                if (allScopes) repository.exportAll(data)
                else repository.exportScope(data, data.preferences.selectedScope)
            }.onSuccess(onReady).onFailure {
                _uiState.value = _uiState.value.copy(message = "导出失败：${it.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun update(transform: (AppData) -> AppData) {
        val updated = transform(_uiState.value.data)
        _uiState.value = _uiState.value.copy(data = updated)
        viewModelScope.launch { repository.save(updated) }
    }
}
