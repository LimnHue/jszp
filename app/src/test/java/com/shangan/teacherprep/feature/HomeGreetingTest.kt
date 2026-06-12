package com.shangan.teacherprep.feature

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGreetingTest {
    @Test
    fun greetingMatchesLocalHourPeriods() {
        assertEquals("夜深了", greetingForHour(2))
        assertEquals("上午好", greetingForHour(8))
        assertEquals("中午好", greetingForHour(12))
        assertEquals("下午好", greetingForHour(15))
        assertEquals("晚上好", greetingForHour(21))
    }
}
