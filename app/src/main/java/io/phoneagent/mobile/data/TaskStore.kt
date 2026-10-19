package io.phoneagent.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/**
 * Minimal local-only persistence. This store has no network client and can only
 * save/read draft records; it intentionally has no API to execute a task.
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
            // Treat damaged local state as empty rather than crashing the app.
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

    private companion object {
        const val PREFERENCES_NAME = "phone_agent_local_v1"
        const val KEY_DRAFTS = "drafts"
        const val MAX_DRAFTS = 100
    }
}
