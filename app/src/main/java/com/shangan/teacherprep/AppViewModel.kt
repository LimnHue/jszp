package com.shangan.teacherprep

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shangan.teacherprep.data.AnswerTemplate
import com.shangan.teacherprep.data.AppData
import com.shangan.teacherprep.data.AppRepository
import com.shangan.teacherprep.data.ContentSection
import com.shangan.teacherprep.data.LibraryScope
import com.shangan.teacherprep.data.PaletteStyle
import com.shangan.teacherprep.data.ScopeConfig
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
        val config = it.scopeConfigs[scope.key] ?: ScopeConfig()
        it.copy(
            preferences = it.preferences.copy(selectedScope = scope),
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

    fun addStage(value: String) = update { data ->
        if (value.isBlank()) data else data.copy(preferences = data.preferences.copy(stages = (data.preferences.stages + value.trim()).distinct()))
    }

    fun addSubject(value: String) = update { data ->
        if (value.isBlank()) data else data.copy(preferences = data.preferences.copy(subjects = (data.preferences.subjects + value.trim()).distinct()))
    }

    fun updateScopeConfig(transform: (ScopeConfig) -> ScopeConfig) = update { data ->
        val key = data.preferences.selectedScope.key
        val config = transform(data.scopeConfigs[key] ?: ScopeConfig())
        data.copy(scopeConfigs = data.scopeConfigs + (key to config))
    }

    fun addTrial(
        title: String,
        textbook: String,
        genre: String,
        markdown: String,
        sections: List<ContentSection>,
        boardImageUri: String?,
    ) = update { data ->
        val scope = data.preferences.selectedScope
        val item = TrialLesson(
            scopeKey = scope.key,
            title = title.ifBlank { "未命名试讲" },
            textbook = textbook,
            genre = genre,
            courseInfoMarkdown = markdown,
            bodySections = sections,
            boardImageUri = boardImageUri,
            durationMinutes = data.preferences.defaultTrialMinutes,
        )
        data.copy(trials = listOf(item) + data.trials)
    }

    fun updateTrial(
        id: String,
        title: String,
        textbook: String,
        genre: String,
        markdown: String,
        sections: List<ContentSection>,
        boardImageUri: String?,
    ) = update { data ->
        data.copy(
            trials = data.trials.map { item ->
                if (item.id != id) item else item.copy(
                    title = title.ifBlank { "未命名试讲" },
                    textbook = textbook,
                    genre = genre,
                    courseInfoMarkdown = markdown,
                    bodySections = sections,
                    boardImageUri = boardImageUri,
                    updatedAt = System.currentTimeMillis(),
                )
            },
        )
    }

    fun addStructured(category: String, question: String, sections: List<ContentSection>) = update { data ->
        val item = StructuredQuestion(
            scopeKey = data.preferences.selectedScope.key,
            category = category,
            question = question.ifBlank { "未命名结构化问题" },
            answerSections = sections,
            durationMinutes = data.preferences.defaultStructuredMinutes,
        )
        data.copy(structuredQuestions = listOf(item) + data.structuredQuestions)
    }

    fun updateStructured(id: String, category: String, question: String, sections: List<ContentSection>) = update { data ->
        data.copy(
            structuredQuestions = data.structuredQuestions.map { item ->
                if (item.id != id) item else item.copy(
                    category = category,
                    question = question.ifBlank { "未命名结构化问题" },
                    answerSections = sections,
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
