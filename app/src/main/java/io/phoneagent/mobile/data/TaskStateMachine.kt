package io.phoneagent.mobile.data

/**
 * Pure transition rules for the future task engine.
 *
 * This class changes task records only. It does not invoke Android APIs, network
 * tools, or an executor. Callers must persist the returned task atomically.
 */
object TaskStateMachine {
    private val allowedTransitions: Map<TaskStatus, Set<TaskStatus>> = mapOf(
        TaskStatus.DRAFT to setOf(TaskStatus.NEEDS_APPROVAL, TaskStatus.STOPPED),
        TaskStatus.NEEDS_APPROVAL to setOf(
            TaskStatus.APPROVED_WAITING_FOR_ENGINE,
            TaskStatus.DRAFT,
            TaskStatus.STOPPED
        ),
        TaskStatus.APPROVED_WAITING_FOR_ENGINE to setOf(
            TaskStatus.RUNNING,
            TaskStatus.STOPPED
        ),
        TaskStatus.RUNNING to setOf(
            TaskStatus.VERIFYING,
            TaskStatus.FAILED,
            TaskStatus.STOPPED
        ),
        TaskStatus.VERIFYING to setOf(
            TaskStatus.COMPLETED,
            TaskStatus.RUNNING,
            TaskStatus.FAILED,
            TaskStatus.STOPPED
        ),
        TaskStatus.COMPLETED to emptySet(),
        TaskStatus.FAILED to setOf(TaskStatus.APPROVED_WAITING_FOR_ENGINE, TaskStatus.STOPPED),
        TaskStatus.STOPPED to emptySet()
    )

    fun canTransition(from: TaskStatus, to: TaskStatus): Boolean =
        to in allowedTransitions[from].orEmpty()

    /**
     * A successful transition appends an audit event and updates the timestamp.
     * Completion is only accepted from VERIFYING and requires verification evidence.
     */
    fun transition(
        task: AgentTask,
        target: TaskStatus,
        nowEpochMillis: Long,
        eventId: String,
        message: String,
        verificationEvidence: String? = null
    ): TaskTransitionResult {
        if (eventId.isBlank()) {
            return TaskTransitionResult.Rejected(task, "Event id must not be blank.")
        }
        if (message.isBlank()) {
            return TaskTransitionResult.Rejected(task, "Transition message must not be blank.")
        }
        if (task.history.any { it.id == eventId }) {
            return TaskTransitionResult.Rejected(task, "Duplicate event id.")
        }
        if (!canTransition(task.status, target)) {
            return TaskTransitionResult.Rejected(
                task,
                "Transition " + task.status + " -> " + target + " is not allowed."
            )
        }
        if (target == TaskStatus.RUNNING &&
            task.status != TaskStatus.APPROVED_WAITING_FOR_ENGINE
        ) {
            return TaskTransitionResult.Rejected(
                task,
                "Execution can start only from an approved task waiting for the engine."
            )
        }
        if (target == TaskStatus.COMPLETED) {
            if (task.status != TaskStatus.VERIFYING) {
                return TaskTransitionResult.Rejected(
                    task,
                    "A task must be in VERIFYING before completion."
                )
            }
            if (verificationEvidence.isNullOrBlank()) {
                return TaskTransitionResult.Rejected(
                    task,
                    "Completion requires non-empty verification evidence."
                )
            }
        }

        val eventType = when (target) {
            TaskStatus.NEEDS_APPROVAL -> "REVIEW_REQUESTED"
            TaskStatus.APPROVED_WAITING_FOR_ENGINE -> "PLAN_APPROVED"
            TaskStatus.RUNNING -> "EXECUTION_STARTED"
            TaskStatus.VERIFYING -> "VERIFICATION_STARTED"
            TaskStatus.COMPLETED -> "EXECUTION_VERIFIED"
            TaskStatus.FAILED -> "EXECUTION_FAILED"
            TaskStatus.STOPPED -> "TASK_STOPPED"
            TaskStatus.DRAFT -> "RETURNED_TO_DRAFT"
        }
        val eventMessage = if (target == TaskStatus.COMPLETED) {
            message + " Evidence: " + verificationEvidence!!.trim()
        } else {
            message
        }
        val event = TaskEvent(
            id = eventId,
            type = eventType,
            message = eventMessage,
            createdAtEpochMillis = nowEpochMillis
        )
        val updated = task.copy(
            status = target,
            history = (task.history + event).takeLast(MAX_EVENTS_PER_TASK_FOR_ENGINE),
            updatedAtEpochMillis = nowEpochMillis
        )
        return TaskTransitionResult.Applied(updated, event)
    }

    private const val MAX_EVENTS_PER_TASK_FOR_ENGINE = 30
}

sealed class TaskTransitionResult {
    data class Applied(
        val task: AgentTask,
        val event: TaskEvent
    ) : TaskTransitionResult()

    data class Rejected(
        val unchangedTask: AgentTask,
        val reason: String
    ) : TaskTransitionResult()
}
