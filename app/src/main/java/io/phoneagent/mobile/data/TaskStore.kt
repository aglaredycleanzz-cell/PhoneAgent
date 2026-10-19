package io.phoneagent.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/**
 * App-private local persistence.
 *
 * This store can persist draft tasks and chat transcript text only. It has no
 * network client, permission-grant API, or device-action executor.
 */
class TaskStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun loadDrafts(): List<AgentTask> {
        val serialized = preferences.getString(KEY_DRAFTS, "[]") ?: "[]"

        return try {
            val array = JSONArray(serialized)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val title = normalizeTaskTitle(item.optString("title"))
                    if (title.isBlank()) continue

                    add(
                        AgentTask(
                            id = id,
                            title = title,
                            createdAtEpochMillis = item.optLong("createdAt", 0L),
                            status = TaskStatus.DRAFT
                        )
                    )
                }
            }.sortedByDescending(AgentTask::createdAtEpochMillis)
        } catch (_: JSONException) {
            emptyList()
        }
    }

    @Synchronized
    fun addDraft(rawTitle: String): AgentTask? {
        val title = normalizeTaskTitle(rawTitle)
        if (title.isBlank()) return null

        val newTask = AgentTask(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        val updated = (listOf(newTask) + loadDrafts()).take(MAX_DRAFTS)
        val encoded = JSONArray()

        updated.forEach { task ->
            encoded.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("createdAt", task.createdAtEpochMillis)
            )
        }

        preferences.edit().putString(KEY_DRAFTS, encoded.toString()).apply()
        return newTask
    }

    @Synchronized
    fun loadConversation(companionId: String): List<ChatMessage> {
        val key = conversationKey(companionId)
        val serialized = preferences.getString(key, "[]") ?: "[]"

        return try {
            val array = JSONArray(serialized)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val text = normalizeChatText(item.optString("text"))
                    if (text.isBlank()) continue

                    add(
                        ChatMessage(
                            id = id,
                            companionId = companionId,
                            text = text,
                            isUser = item.optBoolean("isUser", false),
                            createdAtEpochMillis = item.optLong("createdAt", 0L)
                        )
                    )
                }
            }.takeLast(MAX_CHAT_MESSAGES)
        } catch (_: JSONException) {
            emptyList()
        }
    }

    @Synchronized
    fun addConversationMessage(
        companionId: String,
        rawText: String,
        isUser: Boolean
    ): ChatMessage? {
        val text = normalizeChatText(rawText)
        if (text.isBlank()) return null

        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            companionId = companionId,
            text = text,
            isUser = isUser,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        val updated = (loadConversation(companionId) + message).takeLast(MAX_CHAT_MESSAGES)
        val encoded = JSONArray()

        updated.forEach { item ->
            encoded.put(
                JSONObject()
                    .put("id", item.id)
                    .put("text", item.text)
                    .put("isUser", item.isUser)
                    .put("createdAt", item.createdAtEpochMillis)
            )
        }

        preferences.edit().putString(conversationKey(companionId), encoded.toString()).apply()
        return message
    }

    private fun conversationKey(companionId: String): String {
        val safeId = companionId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(32)
            .ifBlank { "default" }
        return KEY_CHAT_PREFIX + safeId
    }

    private companion object {
        const val PREFERENCES_NAME = "phone_agent_local_v1"
        const val KEY_DRAFTS = "drafts"
        const val KEY_CHAT_PREFIX = "chat_"
        const val MAX_DRAFTS = 100
        const val MAX_CHAT_MESSAGES = 100
    }
}
