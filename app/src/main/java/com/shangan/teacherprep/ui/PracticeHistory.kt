package com.shangan.teacherprep.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val practiceTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

fun practiceHistoryText(practiceCount: Int, lastPracticedAt: Long?): String {
    if (practiceCount <= 0 || lastPracticedAt == null) return "尚未练习"
    val time = Instant.ofEpochMilli(lastPracticedAt)
        .atZone(ZoneId.systemDefault())
        .format(practiceTimeFormatter)
    return "练习 $practiceCount 次 · 最近 $time"
}
