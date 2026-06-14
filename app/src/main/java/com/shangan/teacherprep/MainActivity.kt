package com.shangan.teacherprep

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import com.shangan.teacherprep.data.TrialLesson
import com.shangan.teacherprep.data.PracticeModule
import com.shangan.teacherprep.feature.HomeScreen
import com.shangan.teacherprep.feature.ImportScreen
import com.shangan.teacherprep.feature.LibrarySelectionScreen
import com.shangan.teacherprep.feature.PracticeCalendarScreen
import com.shangan.teacherprep.feature.SettingsScreen
import com.shangan.teacherprep.feature.SettingsDetailScreen
import com.shangan.teacherprep.feature.SettingsSection
import com.shangan.teacherprep.feature.StructuredScreen
import com.shangan.teacherprep.feature.StructuredDetailScreen
import com.shangan.teacherprep.feature.TemplateScreen
import com.shangan.teacherprep.feature.TemplateDetailScreen
import com.shangan.teacherprep.feature.TrialDetailScreen
import com.shangan.teacherprep.feature.TrialLibraryScreen
import com.shangan.teacherprep.ui.MainDestination
import com.shangan.teacherprep.ui.PrepBottomBar
import com.shangan.teacherprep.ui.theme.TeacherPrepTheme
import kotlinx.coroutines.launch

sealed interface AppRoute {
    data object LibrarySelection : AppRoute
    data class Main(val destination: MainDestination) : AppRoute
    data class TrialDetail(
        val lessonId: String,
        val returnToHome: Boolean = false,
        val returnToCalendar: Boolean = false,
    ) : AppRoute
    data class StructuredDetail(
        val questionId: String,
        val returnToHome: Boolean = false,
        val returnToCalendar: Boolean = false,
    ) : AppRoute
    data class TemplateDetail(
        val templateId: String,
        val returnToCalendar: Boolean = false,
    ) : AppRoute
    data class SettingsDetail(val section: SettingsSection) : AppRoute
    data object Calendar : AppRoute
    data class Import(val module: ImportModule, val itemId: String? = null) : AppRoute
}

enum class ImportModule { TRIAL, STRUCTURED, TEMPLATE, BACKUP }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
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

    TeacherPrepTheme(
        paletteStyle = uiState.data.preferences.palette,
        surfaceOpacity = uiState.data.preferences.surfaceOpacity,
        logoScale = uiState.data.preferences.logoScale,
        uiScale = uiState.data.preferences.uiScale,
        fontScale = uiState.data.preferences.fontScale,
    ) {
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
                is AppRoute.StructuredDetail -> if (current.returnToCalendar) {
                    AppRoute.Calendar
                } else {
                    AppRoute.Main(if (current.returnToHome) MainDestination.HOME else MainDestination.STRUCTURED)
                }
                is AppRoute.TemplateDetail -> if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(MainDestination.TEMPLATE)
                is AppRoute.SettingsDetail -> AppRoute.Main(MainDestination.SETTINGS)
                AppRoute.Calendar -> AppRoute.Main(MainDestination.HOME)
                is AppRoute.Import -> returnRoute(current.module, current.itemId)
                is AppRoute.Main -> {
                    if (current.destination == MainDestination.HOME) AppRoute.LibrarySelection
                    else AppRoute.Main(MainDestination.HOME)
                }
            }
        }

        val stateHolder = rememberSaveableStateHolder()
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Box(
                    Modifier.fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background),
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            val contentModifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            AnimatedContent(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                targetState = route!!,
                transitionSpec = {
                    val forward = routeOrder(targetState) >= routeOrder(initialState)
                    (if (forward) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }).using(SizeTransform(clip = false))
                },
                label = "page_transition",
            ) { current ->
                stateHolder.SaveableStateProvider(routeKey(current)) {
                    when (current) {
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
                is AppRoute.Main -> MainPagerScreen(
                    data = uiState.data,
                    initialDestination = current.destination,
                    onRoute = { route = it },
                    onUpdateDrawSelections = vm::updateRandomDrawSelections,
                )
                is AppRoute.SettingsDetail -> SettingsDetailScreen(
                    section = current.section,
                    data = uiState.data,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onBack = { route = AppRoute.Main(MainDestination.SETTINGS) },
                        onPreferences = vm::updatePreferences,
                        onAppearance = vm::updateAppearance,
                        onTrialStartPage = vm::updateTrialStartPage,
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
                        onExport = { vm.exportTrial(lesson.id) { shareMarkdown(context, it, lesson.title) } },
                        onPractice = { vm.recordTrialPractice(lesson.id) },
                        onMediaSaved = { type, path -> vm.addTrialMedia(lesson.id, type, path) },
                        onMediaDelete = { mediaId -> vm.deleteTrialMedia(lesson.id, mediaId) },
                        onMediaRename = { mediaId, name -> vm.renameTrialMedia(lesson.id, mediaId, name) },
                    )
                }
                is AppRoute.StructuredDetail -> {
                    val question = uiState.data.structuredQuestions.firstOrNull { it.id == current.questionId }
                    if (question == null) {
                        route = if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(MainDestination.STRUCTURED)
                    } else {
                        StructuredDetailScreen(
                            question = question,
                            preferences = uiState.data.preferences,
                            modifier = contentModifier,
                            onBack = {
                                route = if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(
                                    if (current.returnToHome) MainDestination.HOME else MainDestination.STRUCTURED,
                                )
                            },
                            onToggleFavorite = { vm.toggleStructuredFavorite(question.id) },
                            onEdit = { route = AppRoute.Import(ImportModule.STRUCTURED, question.id) },
                            onExport = { vm.exportStructured(question.id) { shareMarkdown(context, it, question.question) } },
                            onPractice = { vm.recordStructuredPractice(question.id) },
                            onMediaSaved = { type, path -> vm.addStructuredMedia(question.id, type, path) },
                            onMediaDelete = { mediaId -> vm.deleteStructuredMedia(question.id, mediaId) },
                            onMediaRename = { mediaId, name -> vm.renameStructuredMedia(question.id, mediaId, name) },
                        )
                    }
                }
                is AppRoute.TemplateDetail -> {
                    val template = uiState.data.templates.firstOrNull { it.id == current.templateId }
                    if (template == null) {
                        route = if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(MainDestination.TEMPLATE)
                    } else {
                        TemplateDetailScreen(
                            template = template,
                            preferences = uiState.data.preferences,
                            modifier = contentModifier,
                            onBack = {
                                route = if (current.returnToCalendar) AppRoute.Calendar else AppRoute.Main(MainDestination.TEMPLATE)
                            },
                            onToggleFavorite = { vm.toggleTemplateFavorite(template.id) },
                            onEdit = { route = AppRoute.Import(ImportModule.TEMPLATE, template.id) },
                            onExport = { vm.exportTemplate(template.id) { shareMarkdown(context, it, template.name) } },
                            onPractice = { vm.recordTemplatePractice(template.id) },
                            onMediaSaved = { type, path -> vm.addTemplateMedia(template.id, type, path) },
                            onMediaDelete = { mediaId -> vm.deleteTemplateMedia(template.id, mediaId) },
                            onMediaRename = { mediaId, name -> vm.renameTemplateMedia(template.id, mediaId, name) },
                        )
                    }
                }
                AppRoute.Calendar -> PracticeCalendarScreen(
                    data = uiState.data,
                    onBack = { route = AppRoute.Main(MainDestination.HOME) },
                    onOpenEvent = { event ->
                        route = when (event.module) {
                            PracticeModule.TRIAL -> AppRoute.TrialDetail(event.itemId, returnToCalendar = true)
                            PracticeModule.STRUCTURED -> AppRoute.StructuredDetail(event.itemId, returnToCalendar = true)
                            PracticeModule.TEMPLATE -> AppRoute.TemplateDetail(event.itemId, returnToCalendar = true)
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainPagerScreen(
    data: com.shangan.teacherprep.data.AppData,
    initialDestination: MainDestination,
    onRoute: (AppRoute) -> Unit,
    onUpdateDrawSelections: (PracticeModule, Map<String, Set<String>>) -> Unit,
) {
    val pages = remember {
        listOf(
            MainDestination.HOME,
            MainDestination.TRIAL,
            MainDestination.STRUCTURED,
            MainDestination.TEMPLATE,
            MainDestination.SETTINGS,
        )
    }
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(initialDestination).coerceAtLeast(0),
        pageCount = { pages.size },
    )
    val scope = rememberCoroutineScope()
    val stateHolder = rememberSaveableStateHolder()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            PrepBottomBar(pages[pagerState.currentPage]) { destination ->
                scope.launch { pagerState.animateScrollToPage(pages.indexOf(destination)) }
            }
        },
    ) { pagerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            val destination = pages[page]
            stateHolder.SaveableStateProvider("main_${destination.name}") {
                when (destination) {
                    MainDestination.HOME -> HomeScreen(
                        data = data,
                        contentPadding = pagerPadding,
                        onNavigate = { target ->
                            scope.launch { pagerState.animateScrollToPage(pages.indexOf(target)) }
                        },
                        onSwitchScope = { onRoute(AppRoute.LibrarySelection) },
                        onRandomTrial = {
                            onRoute(AppRoute.TrialDetail(it, returnToHome = true))
                        },
                        onRandomStructured = {
                            onRoute(AppRoute.StructuredDetail(it, returnToHome = true))
                        },
                        onUpdateDrawSelections = onUpdateDrawSelections,
                        onOpenCalendar = { onRoute(AppRoute.Calendar) },
                    )
                    MainDestination.TRIAL -> TrialLibraryScreen(
                        data = data,
                        contentPadding = pagerPadding,
                        onSwitchScope = { onRoute(AppRoute.LibrarySelection) },
                        onOpen = { onRoute(AppRoute.TrialDetail(it)) },
                        onImport = { onRoute(AppRoute.Import(ImportModule.TRIAL)) },
                        onUpdateDrawSelections = onUpdateDrawSelections,
                    )
                    MainDestination.STRUCTURED -> StructuredScreen(
                        data = data,
                        contentPadding = pagerPadding,
                        onSwitchScope = { onRoute(AppRoute.LibrarySelection) },
                        onImport = { onRoute(AppRoute.Import(ImportModule.STRUCTURED)) },
                        onOpen = { onRoute(AppRoute.StructuredDetail(it)) },
                        onUpdateDrawSelections = onUpdateDrawSelections,
                    )
                    MainDestination.TEMPLATE -> TemplateScreen(
                        data = data,
                        contentPadding = pagerPadding,
                        onSwitchScope = { onRoute(AppRoute.LibrarySelection) },
                        onImport = { onRoute(AppRoute.Import(ImportModule.TEMPLATE)) },
                        onOpen = { onRoute(AppRoute.TemplateDetail(it)) },
                        onUpdateDrawSelections = onUpdateDrawSelections,
                    )
                    MainDestination.SETTINGS -> SettingsScreen(
                        data = data,
                        contentPadding = pagerPadding,
                        onOpenSection = { onRoute(AppRoute.SettingsDetail(it)) },
                    )
                }
            }
        }
    }
}

private fun routeOrder(route: AppRoute): Int = when (route) {
    AppRoute.LibrarySelection -> 0
    is AppRoute.Main -> 10 + route.destination.ordinal
    AppRoute.Calendar -> 30
    is AppRoute.TrialDetail,
    is AppRoute.StructuredDetail,
    is AppRoute.TemplateDetail,
    is AppRoute.SettingsDetail
    -> 40
    is AppRoute.Import -> 50
}

private fun routeKey(route: AppRoute): String = when (route) {
    AppRoute.LibrarySelection -> "library_selection"
    is AppRoute.Main -> "main_${route.destination.name}"
    is AppRoute.TrialDetail -> "trial_${route.lessonId}"
    is AppRoute.StructuredDetail -> "structured_${route.questionId}"
    is AppRoute.TemplateDetail -> "template_${route.templateId}"
    is AppRoute.SettingsDetail -> "settings_${route.section.name}"
    AppRoute.Calendar -> "calendar"
    is AppRoute.Import -> "import_${route.module.name}_${route.itemId.orEmpty()}"
}

private fun destinationFor(module: ImportModule): MainDestination = when (module) {
    ImportModule.TRIAL -> MainDestination.TRIAL
    ImportModule.STRUCTURED -> MainDestination.STRUCTURED
    ImportModule.TEMPLATE -> MainDestination.TEMPLATE
    ImportModule.BACKUP -> MainDestination.SETTINGS
}

private fun returnRoute(module: ImportModule, itemId: String?): AppRoute {
    return when {
        itemId == null -> AppRoute.Main(destinationFor(module))
        module == ImportModule.TRIAL -> AppRoute.TrialDetail(itemId)
        module == ImportModule.STRUCTURED -> AppRoute.StructuredDetail(itemId)
        module == ImportModule.TEMPLATE -> AppRoute.TemplateDetail(itemId)
        else -> AppRoute.Main(destinationFor(module))
    }
}

private fun shareMarkdown(context: android.content.Context, uri: android.net.Uri, title: String) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "导出或分享 Markdown",
        ),
    )
}
