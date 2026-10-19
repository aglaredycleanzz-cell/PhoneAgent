package io.phoneagent.mobile.data

/**
 * Task lifecycle model. Runtime states are reserved for the future executor;
 * the current UI can only move a task through review and approval.
 */
data class AgentTask(
    val id: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val status: TaskStatus = TaskStatus.DRAFT,
    val companionId: String = "nova",
    val plan: List<TaskStep> = emptyList(),
    val history: List<TaskEvent> = emptyList(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis
)

enum class TaskStatus {
    DRAFT,
    NEEDS_APPROVAL,
    APPROVED_WAITING_FOR_ENGINE,
    RUNNING,
    VERIFYING,
    COMPLETED,
    FAILED,
    STOPPED
}

enum class StepStatus {
    PLANNED,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}

data class TaskStep(
    val id: String,
    val title: String,
    val detail: String,
    val status: StepStatus = StepStatus.PLANNED
)

data class TaskEvent(
    val id: String,
    val type: String,
    val message: String,
    val createdAtEpochMillis: Long
)

object TaskLifecycle {
    /**
     * Review-only transitions shipped in this version.
     * No UI transition can start or claim completion of device execution.
     */
    fun nextReviewStage(status: TaskStatus): TaskStatus? = when (status) {
        TaskStatus.DRAFT -> TaskStatus.NEEDS_APPROVAL
        TaskStatus.NEEDS_APPROVAL -> TaskStatus.APPROVED_WAITING_FOR_ENGINE
        else -> null
    }

    fun isRuntimeState(status: TaskStatus): Boolean =
        status in setOf(
            TaskStatus.RUNNING,
            TaskStatus.VERIFYING,
            TaskStatus.COMPLETED,
            TaskStatus.FAILED,
            TaskStatus.STOPPED
        )
}

const val MAX_TASK_TITLE_LENGTH = 160
const val MAX_CHAT_MESSAGE_LENGTH = 2000

fun normalizeTaskTitle(raw: String): String {
    val output = StringBuilder()
    var pendingSpace = false

    raw.trim().forEach { character ->
        if (character.isWhitespace()) {
            pendingSpace = output.isNotEmpty()
        } else {
            if (pendingSpace) output.append(' ')
            output.append(character)
            pendingSpace = false
        }
    }

    return output.toString().take(MAX_TASK_TITLE_LENGTH)
}

fun normalizeChatText(raw: String): String =
    raw.trim().take(MAX_CHAT_MESSAGE_LENGTH)
