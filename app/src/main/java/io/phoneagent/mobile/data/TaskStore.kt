package io.phoneagent.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/**
 * App-private persistence for task cards, task history, and companion transcripts.
 *
 * This class intentionally has no network client and no device-action API.
 * Task review can be approved, but execution states can only be written by a
 * future, separately tested execution controller.
 */
class TaskStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun loadDrafts(): List<AgentTask> = loadTasks()

    @Synchronized
    fun loadTasks(): List<AgentTask> {
        if (preferences.contains(KEY_TASKS)) {
            val serialized = preferences.getString(KEY_TASKS, "[]") ?: "[]"
            return parseTasks(serialized)
        }

        // Migrate task drafts made by the first Phone Agent build.
        val legacySerialized = preferences.getString(KEY_DRAFTS, "[]") ?: "[]"
        val migrated = parseLegacyDrafts(legacySerialized)
        if (migrated.isNotEmpty()) saveTasks(migrated)
        return migrated
    }

    @Synchronized
    fun addDraft(rawTitle: String, companionId: String = "nova"): AgentTask? {
        val title = normalizeTaskTitle(rawTitle)
        if (title.isBlank()) return null

        val now = System.currentTimeMillis()
        val safeCompanion = companionId.takeIf {
            it in setOf("nova", "orion", "sentinel")
        } ?: "nova"
        val task = AgentTask(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAtEpochMillis = now,
            status = TaskStatus.DRAFT,
            companionId = safeCompanion,
            plan = TaskPlanFactory.create(title),
            history = listOf(
                TaskEvent(
                    id = UUID.randomUUID().toString(),
                    type = EVENT_CREATED,
                    message = "حُفظ الطلب كمسودة محلية. لم يُنفّذ أي إجراء على الهاتف.",
                    createdAtEpochMillis = now
                )
            ),
            updatedAtEpochMillis = now
        )
        saveTasks((listOf(task) + loadTasks()).take(MAX_TASKS))
        return task
    }

    /**
     * Advances only through review states. Approving a plan never starts device
     * execution; the final review state explicitly waits for a real executor.
     */
    @Synchronized
    fun advanceReviewStage(taskId: String): AgentTask? {
        val tasks = loadTasks()
        val current = tasks.firstOrNull { it.id == taskId } ?: return null
        val next = TaskLifecycle.nextReviewStage(current.status) ?: return null
        val now = System.currentTimeMillis()

        val message = when (next) {
            TaskStatus.NEEDS_APPROVAL -> "أُعدّت خطة أولية؛ يلزم مراجعتها قبل اعتمادها."
            TaskStatus.APPROVED_WAITING_FOR_ENGINE ->
                "اعتمد المستخدم الخطة. التنفيذ ما زال متوقفًا لأن محرك إجراءات الهاتف غير موصول."
            else -> return null
        }

        val event = TaskEvent(
            id = UUID.randomUUID().toString(),
            type = if (next == TaskStatus.NEEDS_APPROVAL) EVENT_REVIEW_REQUESTED else EVENT_PLAN_APPROVED,
            message = message,
            createdAtEpochMillis = now
        )
        val updated = current.copy(
            status = next,
            history = (current.history + event).takeLast(MAX_EVENTS_PER_TASK),
            updatedAtEpochMillis = now
        )
        saveTasks(tasks.map { if (it.id == taskId) updated else it })
        return updated
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

    private fun parseTasks(serialized: String): List<AgentTask> {
        return try {
            val array = JSONArray(serialized)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    parseTask(item)?.let(::add)
                }
            }.sortedByDescending(AgentTask::updatedAtEpochMillis).take(MAX_TASKS)
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun parseLegacyDrafts(serialized: String): List<AgentTask> {
        return try {
            val array = JSONArray(serialized)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val title = normalizeTaskTitle(item.optString("title"))
                    if (title.isBlank()) continue
                    val created = item.optLong("createdAt", 0L)
                    add(
                        AgentTask(
                            id = id,
                            title = title,
                            createdAtEpochMillis = created,
                            status = TaskStatus.DRAFT,
                            companionId = "nova",
                            plan = TaskPlanFactory.create(title),
                            history = listOf(
                                TaskEvent(
                                    id = UUID.randomUUID().toString(),
                                    type = EVENT_MIGRATED,
                                    message = "استُوردت مسودة من إصدار Phone Agent السابق.",
                                    createdAtEpochMillis = created
                                )
                            ),
                            updatedAtEpochMillis = created
                        )
                    )
                }
            }.sortedByDescending(AgentTask::createdAtEpochMillis).take(MAX_TASKS)
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun parseTask(item: JSONObject): AgentTask? {
        val id = item.optString("id").takeIf(String::isNotBlank) ?: return null
        val title = normalizeTaskTitle(item.optString("title"))
        if (title.isBlank()) return null

        val createdAt = item.optLong("createdAt", 0L)
        val status = runCatching {
            TaskStatus.valueOf(item.optString("status", TaskStatus.DRAFT.name))
        }.getOrDefault(TaskStatus.DRAFT)
        val companion = item.optString("companionId", "nova")
            .takeIf { it in setOf("nova", "orion", "sentinel") } ?: "nova"

        val planArray = item.optJSONArray("plan") ?: JSONArray()
        val plan = buildList {
            for (index in 0 until planArray.length()) {
                val step = planArray.optJSONObject(index) ?: continue
                val stepId = step.optString("id").takeIf(String::isNotBlank) ?: continue
                val stepTitle = normalizeTaskTitle(step.optString("title"))
                if (stepTitle.isBlank()) continue
                val stepStatus = runCatching {
                    StepStatus.valueOf(step.optString("status", StepStatus.PLANNED.name))
                }.getOrDefault(StepStatus.PLANNED)
                add(
                    TaskStep(
                        id = stepId,
                        title = stepTitle,
                        detail = step.optString("detail"),
                        status = stepStatus
                    )
                )
            }
        }

        val historyArray = item.optJSONArray("history") ?: JSONArray()
        val history = buildList {
            for (index in 0 until historyArray.length()) {
                val event = historyArray.optJSONObject(index) ?: continue
                val eventId = event.optString("id").takeIf(String::isNotBlank) ?: continue
                val message = event.optString("message")
                if (message.isBlank()) continue
                add(
                    TaskEvent(
                        id = eventId,
                        type = event.optString("type", "NOTE"),
                        message = message,
                        createdAtEpochMillis = event.optLong("createdAt", createdAt)
                    )
                )
            }
        }.takeLast(MAX_EVENTS_PER_TASK)

        return AgentTask(
            id = id,
            title = title,
            createdAtEpochMillis = createdAt,
            status = status,
            companionId = companion,
            plan = plan.ifEmpty { TaskPlanFactory.create(title) },
            history = history,
            updatedAtEpochMillis = item.optLong("updatedAt", createdAt)
        )
    }

    private fun saveTasks(tasks: List<AgentTask>) {
        val encoded = JSONArray()
        tasks.take(MAX_TASKS).forEach { task ->
            val plan = JSONArray()
            task.plan.forEach { step ->
                plan.put(
                    JSONObject()
                        .put("id", step.id)
                        .put("title", step.title)
                        .put("detail", step.detail)
                        .put("status", step.status.name)
                )
            }
            val history = JSONArray()
            task.history.takeLast(MAX_EVENTS_PER_TASK).forEach { event ->
                history.put(
                    JSONObject()
                        .put("id", event.id)
                        .put("type", event.type)
                        .put("message", event.message)
                        .put("createdAt", event.createdAtEpochMillis)
                )
            }

            encoded.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("createdAt", task.createdAtEpochMillis)
                    .put("updatedAt", task.updatedAtEpochMillis)
                    .put("status", task.status.name)
                    .put("companionId", task.companionId)
                    .put("plan", plan)
                    .put("history", history)
            )
        }
        preferences.edit().putString(KEY_TASKS, encoded.toString()).apply()
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
        const val KEY_TASKS = "tasks_v2"
        const val KEY_CHAT_PREFIX = "chat_"
        const val MAX_TASKS = 100
        const val MAX_CHAT_MESSAGES = 100
        const val MAX_EVENTS_PER_TASK = 30

        const val EVENT_CREATED = "CREATED"
        const val EVENT_MIGRATED = "MIGRATED"
        const val EVENT_REVIEW_REQUESTED = "REVIEW_REQUESTED"
        const val EVENT_PLAN_APPROVED = "PLAN_APPROVED"
    }
}
