package com.vidly.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameParsingTest {

    private val repo = DownloadRepository()

    @Test
    fun `extracts filename from content disposition`() {
        assertEquals("clip.mp4", repo.fileName("attachment; filename=\"clip.mp4\""))
    }

    @Test
    fun `falls back to generated name when header useless`() {
        val generated = repo.fileName(null)
        assertTrue(generated.startsWith("vidly_"))
        assertTrue(generated.endsWith(".mp4"))
        assertTrue(repo.fileName("attachment").startsWith("vidly_"))
        assertTrue(repo.fileName("attachment; filename=\"\"").startsWith("vidly_"))
    }
}
