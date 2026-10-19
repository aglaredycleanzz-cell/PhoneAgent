package io.phoneagent.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentTaskTest {
    @Test
    fun normalizeTaskTitleTrimsAndCollapsesWhitespace() {
        assertEquals(
            "افحص ملفات المشروع",
            normalizeTaskTitle("   افحص   ملفات\n المشروع\t ")
        )
    }

    @Test
    fun normalizeTaskTitleReturnsEmptyForWhitespaceOnlyInput() {
        assertEquals("", normalizeTaskTitle(" \n\t "))
    }

    @Test
    fun normalizeTaskTitleEnforcesMaximumLength() {
        assertEquals(MAX_TASK_TITLE_LENGTH, normalizeTaskTitle("x".repeat(400)).length)
    }
}
