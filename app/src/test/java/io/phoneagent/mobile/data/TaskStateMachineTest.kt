package io.phoneagent.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskStateMachineTest {
    private fun task(status: TaskStatus) = AgentTask(
        id = "task-1",
        title = "اختبار محرك المهام",
        createdAtEpochMillis = 100L,
        status = status,
        history = listOf(
            TaskEvent("created", "CREATED", "تم إنشاء المهمة", 100L)
        ),
        updatedAtEpochMillis = 100L
    )

    @Test
    fun approvedTaskMayEnterRunning() {
        assertTrue(
            TaskStateMachine.canTransition(
                TaskStatus.APPROVED_WAITING_FOR_ENGINE,
                TaskStatus.RUNNING
            )
        )
    }

    @Test
    fun draftCannotSkipApprovalAndStartRunning() {
        assertFalse(TaskStateMachine.canTransition(TaskStatus.DRAFT, TaskStatus.RUNNING))
        val original = task(TaskStatus.DRAFT)
        val result = TaskStateMachine.transition(
            original, TaskStatus.RUNNING, 200L, "event-1", "بدء التنفيذ"
        )
        assertTrue(result is TaskTransitionResult.Rejected)
        assertEquals(original, (result as TaskTransitionResult.Rejected).unchangedTask)
    }

    @Test
    fun completionRequiresVerificationEvidence() {
        val original = task(TaskStatus.VERIFYING)
        val result = TaskStateMachine.transition(
            original, TaskStatus.COMPLETED, 200L, "event-1", "تم التحقق"
        )
        assertTrue(result is TaskTransitionResult.Rejected)
        assertEquals(original, (result as TaskTransitionResult.Rejected).unchangedTask)
    }

    @Test
    fun verifiedCompletionStoresEvidenceAndAuditEvent() {
        val original = task(TaskStatus.VERIFYING)
        val result = TaskStateMachine.transition(
            original,
            TaskStatus.COMPLETED,
            250L,
            "event-verified",
            "اكتمل التحقق",
            "تم العثور على الملف المتوقع"
        )
        assertTrue(result is TaskTransitionResult.Applied)
        val updated = (result as TaskTransitionResult.Applied).task
        assertEquals(TaskStatus.COMPLETED, updated.status)
        assertEquals(250L, updated.updatedAtEpochMillis)
        assertTrue(updated.history.last().message.contains("تم العثور على الملف المتوقع"))
        assertEquals("EXECUTION_VERIFIED", updated.history.last().type)
    }

    @Test
    fun duplicateEventIdIsRejectedWithoutMutation() {
        val original = task(TaskStatus.APPROVED_WAITING_FOR_ENGINE)
        val result = TaskStateMachine.transition(
            original,
            TaskStatus.RUNNING,
            250L,
            "created",
            "بدء التنفيذ"
        )
        assertTrue(result is TaskTransitionResult.Rejected)
        assertEquals(original, (result as TaskTransitionResult.Rejected).unchangedTask)
    }

    @Test
    fun terminalCompletedTaskCannotTransitionAgain() {
        assertFalse(TaskStateMachine.canTransition(TaskStatus.COMPLETED, TaskStatus.RUNNING))
        assertFalse(TaskStateMachine.canTransition(TaskStatus.COMPLETED, TaskStatus.STOPPED))
    }

    @Test
    fun stoppedTaskCannotResume() {
        assertFalse(TaskStateMachine.canTransition(TaskStatus.STOPPED, TaskStatus.RUNNING))
        assertFalse(TaskStateMachine.canTransition(TaskStatus.STOPPED, TaskStatus.DRAFT))
    }

    @Test
    fun transitionRecordsEventAndTimestamp() {
        val original = task(TaskStatus.APPROVED_WAITING_FOR_ENGINE)
        val result = TaskStateMachine.transition(
            original, TaskStatus.RUNNING, 333L, "event-start", "بدأ المنفّذ العمل"
        )
        assertTrue(result is TaskTransitionResult.Applied)
        val updated = (result as TaskTransitionResult.Applied).task
        assertEquals(333L, updated.updatedAtEpochMillis)
        assertEquals("EXECUTION_STARTED", updated.history.last().type)
        assertEquals(2, updated.history.size)
    }
}
