package io.phoneagent.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun draftCanMoveToApprovalReview() {
        assertEquals(TaskStatus.NEEDS_APPROVAL, TaskLifecycle.nextReviewStage(TaskStatus.DRAFT))
    }

    @Test
    fun reviewedTaskCanWaitForFutureExecutionEngine() {
        assertEquals(
            TaskStatus.APPROVED_WAITING_FOR_ENGINE,
            TaskLifecycle.nextReviewStage(TaskStatus.NEEDS_APPROVAL)
        )
    }

    @Test
    fun approvalDoesNotStartExecution() {
        assertNull(TaskLifecycle.nextReviewStage(TaskStatus.APPROVED_WAITING_FOR_ENGINE))
        assertFalse(TaskStatus.APPROVED_WAITING_FOR_ENGINE == TaskStatus.RUNNING)
    }

    @Test
    fun runtimeStatusesAreDistinctFromDraftAndApprovalStatuses() {
        assertTrue(TaskLifecycle.isRuntimeState(TaskStatus.RUNNING))
        assertTrue(TaskLifecycle.isRuntimeState(TaskStatus.COMPLETED))
        assertFalse(TaskLifecycle.isRuntimeState(TaskStatus.DRAFT))
        assertFalse(TaskLifecycle.isRuntimeState(TaskStatus.APPROVED_WAITING_FOR_ENGINE))
    }

    @Test
    fun plannerReturnsClearlyPlannedSteps() {
        val steps = TaskPlanFactory.create("افحص ملفات المشروع وابن APK")
        assertEquals(3, steps.size)
        assertTrue(steps.all { it.status == StepStatus.PLANNED })
        assertTrue(steps.first().title.contains("المدخلات"))
    }
}
