package com.shangan.teacherprep.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LibraryScope(
    val stage: String = "初中",
    val subject: String = "语文",
) {
    val key: String get() = "$stage::$subject"
}

@Serializable
data class ContentSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val markdown: String,
)

@Serializable
data class TrialLesson(
    val id: String = UUID.randomUUID().toString(),
    val scopeKey: String,
    val title: String,
    val author: String = "",
    val textbook: String,
    val genre: String,
    val courseInfoMarkdown: String,
    val bodySections: List<ContentSection>,
    val boardImageUri: String? = null,
    val durationMinutes: Int = 15,
    val progress: Float = 0f,
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
    val favorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ScopeConfig(
    val textbooks: List<String> = listOf("七年级上册", "七年级下册", "八年级上册", "八年级下册", "九年级上册", "九年级下册"),
    val genres: List<String> = listOf("写景散文", "叙事散文", "小说", "古诗文", "说明文"),
    val structuredTypes: List<String> = listOf("教育教学", "应急应变", "人际沟通", "综合分析"),
    val templateTypes: List<String> = listOf("导入语", "过渡语", "评价语", "答题框架"),
    val trialSectionNames: List<String> = listOf("导入新课", "整体感知", "品读赏析", "课堂小结"),
    val structuredSectionNames: List<String> = listOf("答题思路", "参考答案"),
)

@Serializable
enum class TimerMode { COUNTDOWN, STOPWATCH }

@Serializable
enum class PaletteStyle { CORAL, SKY, PINK, VIOLET, MINT }

@Serializable
data class AppPreferences(
    val selectedScope: LibraryScope = LibraryScope(),
    val stages: List<String> = listOf("幼儿园", "小学", "初中", "高中", "中职"),
    val subjects: List<String> = listOf(
        "语文", "数学", "英语", "物理", "化学", "生物", "道德与法治",
        "历史", "地理", "科学", "信息技术", "音乐", "美术", "体育", "心理健康",
    ),
    val timerMode: TimerMode = TimerMode.COUNTDOWN,
    val defaultTrialMinutes: Int = 15,
    val defaultStructuredMinutes: Int = 5,
    val palette: PaletteStyle = PaletteStyle.CORAL,
    val surfaceOpacity: Float = 0.96f,
)

@Serializable
data class AppData(
    val schemaVersion: Int = 1,
    val preferences: AppPreferences = AppPreferences(),
    val scopeConfigs: Map<String, ScopeConfig> = emptyMap(),
    val trials: List<TrialLesson> = emptyList(),
    val structuredQuestions: List<StructuredQuestion> = emptyList(),
    val templates: List<AnswerTemplate> = emptyList(),
)
