package com.shangan.teacherprep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopeDefaultsTest {
    @Test
    fun `high school Chinese uses high school textbook categories`() {
        val config = ScopeDefaults.create(LibraryScope("高中", "语文", "人教版"))

        assertEquals("必修上册", config.textbooks.first())
        assertTrue(config.textbooks.contains("选择性必修下册"))
        assertTrue(config.textbooks.none { it.contains("七年级") })
    }

    @Test
    fun `textbook version creates a distinct library key`() {
        val renjiao = LibraryScope("初中", "语文", "人教版")
        val sujiao = LibraryScope("初中", "语文", "苏教版")

        assertTrue(renjiao.key != sujiao.key)
    }
}
