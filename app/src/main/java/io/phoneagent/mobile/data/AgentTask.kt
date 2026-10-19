package io.phoneagent.mobile.data

/**
 * A task in version 0.1 is only a user-authored draft.
 * It has no executable command, permission grant, or execution state.
 */
data class AgentTask(
    val id: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val status: TaskStatus = TaskStatus.DRAFT
)

enum class TaskStatus {
    DRAFT
}

const val MAX_TASK_TITLE_LENGTH = 160

/**
 * Normalizes user input without interpreting it as an executable instruction.
 */
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
