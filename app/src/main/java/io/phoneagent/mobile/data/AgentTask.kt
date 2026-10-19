package io.phoneagent.mobile.data

/**
 * A task is user-authored text until a real, permission-gated executor is implemented.
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

data class ChatMessage(
    val id: String,
    val companionId: String,
    val text: String,
    val isUser: Boolean,
    val createdAtEpochMillis: Long
)

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
