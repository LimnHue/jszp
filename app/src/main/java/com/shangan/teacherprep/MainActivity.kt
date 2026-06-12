package com.shangan.teacherprep

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shangan.teacherprep.data.TrialLesson
import com.shangan.teacherprep.data.PracticeModule
import com.shangan.teacherprep.feature.HomeScreen
import com.shangan.teacherprep.feature.ImportScreen
import com.shangan.teacherprep.feature.LibrarySelectionScreen
import com.shangan.teacherprep.feature.PracticeCalendarScreen
import com.shangan.teacherprep.feature.SettingsScreen
import com.shangan.teacherprep.feature.StructuredScreen
import com.shangan.teacherprep.feature.TemplateScreen
import com.shangan.teacherprep.feature.TrialDetailScreen
import com.shangan.teacherprep.feature.TrialLibraryScreen
import com.shangan.teacherprep.ui.MainDestination
import com.shangan.teacherprep.ui.PrepBottomBar
import com.shangan.teacherprep.ui.theme.TeacherPrepTheme

sealed interface AppRoute {
    data object LibrarySelection : AppRoute
    data class Main(val destination: MainDestination) : AppRoute
    data class TrialDetail(
        val lessonId: String,
        val returnToHome: Boolean = false,
        val returnToCalendar: Boolean = false,
    ) : AppRoute
    data class StructuredDraw(val questionId: String, val returnToCalendar: Boolean = false) : AppRoute
    data class TemplateFocus(val templateId: String) : AppRoute
    data object Calendar : AppRoute
    data class Import(val module: ImportModule, val itemId: String? = null) : AppRoute
}

enum class ImportModule { TRIAL, STRUCTURED, TEMPLATE, BACKUP }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TeacherPrepRoot() }
    }
}

@Composable
private fun TeacherPrepRoot(vm: AppViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var route: AppRoute? by remember { mutableStateOf(null) }
    val snackbar = remember { SnackbarHostState() }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importBackup)
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(uiState.loading) {
        if (!uiState.loading && route == null) {
            route = if (uiState.data.preferences.hasCompletedLibrarySelection) {
                AppRoute.Main(MainDestination.HOME)
            } else {
                AppRoute.LibrarySelection
            }
        }
    }

    TeacherPrepTheme(uiState.data.preferences.palette, uiState.data.preferences.surfaceOpacity) {
        if (uiState.loading) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@TeacherPrepTheme
        }

        if (route == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@TeacherPrepTheme
        }

        BackHandler(enabled = route != AppRoute.LibrarySelection) {
            route = when (val current = route) {
                null -> AppRoute.LibrarySelection
                AppRoute.LibrarySelection -> AppRoute.LibrarySelection
                is AppRoute.TrialDetail -> if (current.returnToCalendar) {
                    AppRoute.Calendar
                } else {
                    AppRoute.Main(if (current.returnToHome) MainDestination.HOME else MainDestination.TRIAL)
                }
                is AppRoute.StructuredDraw -> if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(MainDestination.HOME)
                is AppRoute.TemplateFocus -> AppRoute.Calendar
                AppRoute.Calendar -> AppRoute.Main(MainDestination.HOME)
                is AppRoute.Import -> returnRoute(current.module, current.itemId)
                is AppRoute.Main -> {
                    if (current.destination == MainDestination.HOME) AppRoute.LibrarySelection
                    else AppRoute.Main(MainDestination.HOME)
                }
            }
        }

        val currentMain = (route as? AppRoute.Main)?.destination
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (currentMain != null) {
                    PrepBottomBar(currentMain) { route = AppRoute.Main(it) }
                }
            },
        ) { padding ->
            val contentModifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            when (val current = route) {
                null -> Unit
                AppRoute.LibrarySelection -> LibrarySelectionScreen(
                    data = uiState.data,
                    modifier = contentModifier,
                    onAddStage = vm::addStage,
                    onAddSubject = vm::addSubject,
                    onAddTextbookVersion = vm::addTextbookVersion,
                    onEnter = {
                        vm.selectScope(it)
                        route = AppRoute.Main(MainDestination.HOME)
                    },
                )
                is AppRoute.Main -> when (current.destination) {
                    MainDestination.HOME -> HomeScreen(
                        data = uiState.data,
                        contentPadding = padding,
                        onNavigate = { route = AppRoute.Main(it) },
                        onSwitchScope = { route = AppRoute.LibrarySelection },
                        onRandomTrial = {
                            randomTrial(uiState.data)?.let {
                                route = AppRoute.TrialDetail(it.id, returnToHome = true)
                            }
                        },
                        onRandomStructured = {
                            randomStructured(uiState.data)?.let {
                                route = AppRoute.StructuredDraw(it.id)
                            }
                        },
                        onOpenCalendar = { route = AppRoute.Calendar },
                    )
                    MainDestination.TRIAL -> TrialLibraryScreen(
                        data = uiState.data,
                        contentPadding = padding,
                        onSwitchScope = { route = AppRoute.LibrarySelection },
                        onOpen = { route = AppRoute.TrialDetail(it) },
                        onImport = { route = AppRoute.Import(ImportModule.TRIAL) },
                    )
                    MainDestination.STRUCTURED -> StructuredScreen(
                        data = uiState.data,
                        contentPadding = padding,
                        onSwitchScope = { route = AppRoute.LibrarySelection },
                        onImport = { route = AppRoute.Import(ImportModule.STRUCTURED) },
                        onToggleFavorite = vm::toggleStructuredFavorite,
                        onEdit = { route = AppRoute.Import(ImportModule.STRUCTURED, it) },
                        onPractice = vm::recordStructuredPractice,
                    )
                    MainDestination.TEMPLATE -> TemplateScreen(
                        data = uiState.data,
                        contentPadding = padding,
                        onSwitchScope = { route = AppRoute.LibrarySelection },
                        onImport = { route = AppRoute.Import(ImportModule.TEMPLATE) },
                        onToggleFavorite = vm::toggleTemplateFavorite,
                        onEdit = { route = AppRoute.Import(ImportModule.TEMPLATE, it) },
                        onPractice = vm::recordTemplatePractice,
                    )
                    MainDestination.SETTINGS -> SettingsScreen(
                        data = uiState.data,
                        contentPadding = padding,
                        onPreferences = vm::updatePreferences,
                        onReminderPreferences = vm::updateReminderPreferences,
                        onFilterVisibility = vm::updateFilterVisibility,
                        onUpdateConfig = vm::updateScopeConfig,
                        onImportBackup = { backupPicker.launch(arrayOf("application/json", "text/json")) },
                        onExport = { all ->
                            vm.exportLibrary(all) { uri ->
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        },
                                        "导出或分享备考库",
                                    ),
                                )
                            }
                        },
                    )
                }
                is AppRoute.TrialDetail -> {
                    val lesson = uiState.data.trials.firstOrNull { it.id == current.lessonId }
                    if (lesson == null) {
                        route = if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(
                            if (current.returnToHome) MainDestination.HOME else MainDestination.TRIAL,
                        )
                    }
                    else TrialDetailScreen(
                        lesson = lesson,
                        preferences = uiState.data.preferences,
                        modifier = contentModifier,
                        onBack = {
                            route = if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(
                                if (current.returnToHome) MainDestination.HOME else MainDestination.TRIAL,
                            )
                        },
                        onToggleFavorite = { vm.toggleTrialFavorite(lesson.id) },
                        onEdit = { route = AppRoute.Import(ImportModule.TRIAL, lesson.id) },
                        onPractice = { vm.recordTrialPractice(lesson.id) },
                        onMediaSaved = { type, path -> vm.addTrialMedia(lesson.id, type, path) },
                    )
                }
                is AppRoute.StructuredDraw -> StructuredScreen(
                    data = uiState.data,
                    contentPadding = padding,
                    onSwitchScope = { route = AppRoute.LibrarySelection },
                    onImport = { route = AppRoute.Import(ImportModule.STRUCTURED) },
                    onToggleFavorite = vm::toggleStructuredFavorite,
                    onEdit = { route = AppRoute.Import(ImportModule.STRUCTURED, it) },
                    onPractice = vm::recordStructuredPractice,
                    initialSelectedId = current.questionId,
                    drawMode = true,
                    onBack = {
                        route = if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(MainDestination.HOME)
                    },
                )
                is AppRoute.TemplateFocus -> TemplateScreen(
                    data = uiState.data,
                    contentPadding = padding,
                    onSwitchScope = { route = AppRoute.LibrarySelection },
                    onImport = { route = AppRoute.Import(ImportModule.TEMPLATE) },
                    onToggleFavorite = vm::toggleTemplateFavorite,
                    onEdit = { route = AppRoute.Import(ImportModule.TEMPLATE, it) },
                    onPractice = vm::recordTemplatePractice,
                    initialExpandedId = current.templateId,
                    focusMode = true,
                    onBack = { route = AppRoute.Calendar },
                )
                AppRoute.Calendar -> PracticeCalendarScreen(
                    data = uiState.data,
                    onBack = { route = AppRoute.Main(MainDestination.HOME) },
                    onOpenEvent = { event ->
                        route = when (event.module) {
                            PracticeModule.TRIAL -> AppRoute.TrialDetail(event.itemId, returnToCalendar = true)
                            PracticeModule.STRUCTURED -> AppRoute.StructuredDraw(event.itemId, returnToCalendar = true)
                            PracticeModule.TEMPLATE -> AppRoute.TemplateFocus(event.itemId)
                        }
                    },
                )
                is AppRoute.Import -> ImportScreen(
                    module = current.module,
                    editingId = current.itemId,
                    data = uiState.data,
                    modifier = contentModifier,
                    onBack = { route = returnRoute(current.module, current.itemId) },
                    onAddTrial = vm::addTrial,
                    onAddStructured = vm::addStructured,
                    onAddTemplate = vm::addTemplate,
                    onUpdateTrial = vm::updateTrial,
                    onUpdateStructured = vm::updateStructured,
                    onUpdateTemplate = vm::updateTemplate,
                    onComplete = { route = returnRoute(current.module, current.itemId) },
                )
            }
        }
    }
}

private fun randomTrial(data: com.shangan.teacherprep.data.AppData): TrialLesson? {
    val key = data.preferences.selectedScope.key
    return data.trials.filter { it.scopeKey == key }.randomOrNull()
}

private fun randomStructured(data: com.shangan.teacherprep.data.AppData) =
    data.structuredQuestions
        .filter { it.scopeKey == data.preferences.selectedScope.key }
        .randomOrNull()

private fun destinationFor(module: ImportModule): MainDestination = when (module) {
    ImportModule.TRIAL -> MainDestination.TRIAL
    ImportModule.STRUCTURED -> MainDestination.STRUCTURED
    ImportModule.TEMPLATE -> MainDestination.TEMPLATE
    ImportModule.BACKUP -> MainDestination.SETTINGS
}

private fun returnRoute(module: ImportModule, itemId: String?): AppRoute {
    return if (module == ImportModule.TRIAL && itemId != null) {
        AppRoute.TrialDetail(itemId)
    } else {
        AppRoute.Main(destinationFor(module))
    }
}
