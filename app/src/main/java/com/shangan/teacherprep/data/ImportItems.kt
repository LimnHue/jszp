package com.shangan.teacherprep.data

data class StructuredImportItem(
    val question: String,
    val answerSections: List<ContentSection>,
)

data class TrialImportItem(
    val title: String,
    val courseInfoMarkdown: String,
    val bodySections: List<ContentSection>,
)
