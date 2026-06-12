package com.shangan.teacherprep.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LibraryScope(
    val stage: String = "初中",
    val subject: String = "语文",
    val textbookVersion: String = "人教版",
) {
    val key: String get() = "$stage::$subject::$textbookVersion"
}

@Serializable
data class ContentSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val markdown: String,
)

@Serializable
enum class PracticeModule { TRIAL, STRUCTURED, TEMPLATE }

@Serializable
enum class PracticeMediaType { VIDEO, AUDIO }

@Serializable
data class PracticeMedia(
    val id: String = UUID.randomUUID().toString(),
    val type: PracticeMediaType,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class PracticeEvent(
    val id: String = UUID.randomUUID().toString(),
    val scopeKey: String,
    val module: PracticeModule,
    val itemId: String,
    val title: String,
    val practicedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class TrialLesson(
    val id: String = UUID.randomUUID().toString(),
    val scopeKey: String,
    val title: String,
    val author: String = "",
    val textbook: String,
    val unit: String = "",
    val lessonOrder: Int = 0,
    val genre: String,
    val courseInfoMarkdown: String,
    val bodySections: List<ContentSection>,
    val boardImageUri: String? = null,
    val durationMinutes: Int = 15,
    val importance: Int = 3,
    val practiceCount: Int = 0,
    val lastPracticedAt: Long? = null,
    val practiceMedia: List<PracticeMedia> = emptyList(),
    val favorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class StructuredQuestion(
    val id: String = UUID.randomUUID().toString(),
    val scopeKey: String,
    val category: String,
    val question: String,
    val answerSections: List<ContentSection>,
    val durationMinutes: Int = 5,
    val importance: Int = 3,
    val practiceCount: Int = 0,
    val lastPracticedAt: Long? = null,
    val favorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class AnswerTemplate(
    val id: String = UUID.randomUUID().toString(),
    val scopeKey: String,
    val category: String,
    val name: String,
    val summary: String,
    val contentMarkdown: String,
    val module: String = "通用",
    val practiceCount: Int = 0,
    val lastPracticedAt: Long? = null,
    val favorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ScopeConfig(
    val textbooks: List<String> = listOf("七年级上册", "七年级下册", "八年级上册", "八年级下册", "九年级上册", "九年级下册"),
    val units: List<String> = listOf("第一单元", "第二单元", "第三单元", "第四单元", "第五单元", "第六单元", "其他"),
    val genres: List<String> = listOf("写景散文", "叙事散文", "小说", "古诗文", "说明文"),
    val structuredTypes: List<String> = listOf("教育教学", "应急应变", "人际沟通", "综合分析"),
    val templateTypes: List<String> = listOf("导入语", "过渡语", "评价语", "答题框架"),
    val trialSectionNames: List<String> = listOf("导入新课", "整体感知", "品读赏析", "课堂小结"),
    val structuredSectionNames: List<String> = listOf("答题思路", "参考答案"),
)

object ScopeDefaults {
    fun create(scope: LibraryScope): ScopeConfig {
        val textbooks = when {
            scope.stage == "幼儿园" -> listOf("小班上册", "小班下册", "中班上册", "中班下册", "大班上册", "大班下册")
            scope.stage == "小学" -> (1..6).flatMap { grade -> listOf("${chineseNumber(grade)}年级上册", "${chineseNumber(grade)}年级下册") }
            scope.stage == "初中" -> listOf("七年级上册", "七年级下册", "八年级上册", "八年级下册", "九年级上册", "九年级下册")
            scope.stage == "高中" && scope.subject == "语文" -> listOf("必修上册", "必修下册", "选择性必修上册", "选择性必修中册", "选择性必修下册")
            scope.stage == "高中" -> listOf("必修第一册", "必修第二册", "选择性必修第一册", "选择性必修第二册", "选择性必修第三册")
            scope.stage == "中职" -> listOf("基础模块上册", "基础模块下册", "职业模块", "拓展模块")
            else -> listOf("上册", "下册")
        }
        val genres = when (scope.subject) {
            "语文" -> listOf("写景散文", "叙事散文", "写人散文", "小说", "诗歌", "古诗文", "说明文", "议论文", "新闻演讲", "寓言")
            "英语" -> listOf("听说课", "阅读课", "写作课", "语法课", "词汇课", "综合课")
            "数学" -> listOf("概念课", "计算课", "探究课", "复习课", "习题课")
            "物理", "化学", "生物", "科学" -> listOf("概念课", "实验课", "探究课", "复习课", "习题课")
            else -> listOf("新授课", "活动课", "探究课", "复习课", "综合课")
        }
        val unitCount = if (scope.stage == "小学") 8 else 6
        return ScopeConfig(
            textbooks = textbooks,
            units = (1..unitCount).map { "第${chineseNumber(it)}单元" } + "其他",
            genres = genres,
        )
    }

    private fun chineseNumber(value: Int): String = when (value) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "七"
        8 -> "八"
        else -> value.toString()
    }
}

@Serializable
enum class TimerMode { COUNTDOWN, STOPWATCH }

@Serializable
enum class PaletteStyle { CORAL, SKY, PINK, VIOLET, MINT }

@Serializable
data class FilterVisibility(
    val trialSearch: Boolean = true,
    val trialTextbook: Boolean = true,
    val trialUnit: Boolean = true,
    val trialGenre: Boolean = true,
    val trialImportance: Boolean = true,
    val structuredCategory: Boolean = true,
    val structuredImportance: Boolean = true,
    val templateSearch: Boolean = true,
    val templateCategory: Boolean = true,
)

@Serializable
data class AppPreferences(
    val selectedScope: LibraryScope = LibraryScope(),
    val hasCompletedLibrarySelection: Boolean = false,
    val stages: List<String> = listOf("幼儿园", "小学", "初中", "高中", "中职"),
    val subjects: List<String> = listOf(
        "语文", "数学", "英语", "物理", "化学", "生物", "道德与法治",
        "历史", "地理", "科学", "信息技术", "音乐", "美术", "体育", "心理健康",
    ),
    val textbookVersions: List<String> = listOf("人教版", "统编版", "北师大版", "苏教版", "教科版", "外研版", "其他"),
    val timerMode: TimerMode = TimerMode.COUNTDOWN,
    val defaultTrialMinutes: Int = 15,
    val defaultStructuredMinutes: Int = 5,
    val remindBeforeEnd: Boolean = true,
    val reminderMinutesBeforeEnd: Int = 5,
    val remindAtEnd: Boolean = true,
    val filterVisibility: FilterVisibility = FilterVisibility(),
    val palette: PaletteStyle = PaletteStyle.CORAL,
    val surfaceOpacity: Float = 0.96f,
)

@Serializable
data class AppData(
    val schemaVersion: Int = 8,
    val preferences: AppPreferences = AppPreferences(),
    val scopeConfigs: Map<String, ScopeConfig> = emptyMap(),
    val trials: List<TrialLesson> = emptyList(),
    val structuredQuestions: List<StructuredQuestion> = emptyList(),
    val templates: List<AnswerTemplate> = emptyList(),
    val practiceEvents: List<PracticeEvent> = emptyList(),
)
