# Architecture

## Product interface

The primary surface is a conversation with three role-based companions:
NOVA (planner), ORION (technical helper), and SENTINEL (safety reviewer).
The personas have different local response rules and visual treatments. They
are not separate language models in the current build.

## Current task data flow

```
Arabic conversation
        |
        v
LocalCompanion action-request heuristic
        |
        v
TaskStore.addDraft(request, companionId)
        |
        +--> TaskPlanFactory: three deterministic checklist steps
        |
        +--> app-private SharedPreferences (tasks_v2 JSON)
                    |
                    +--> task status
                    +--> plan checklist
                    +--> append-only status history
```

The task plan is a deterministic template, not AI-generated planning and not an
executable command sequence. Conversation and task data stay in the app-private
SharedPreferences in this version.

## Task lifecycle

- `DRAFT`: request captured locally.
- `NEEDS_APPROVAL`: the user moved the card to the review/approval stage.
- `APPROVED_WAITING_FOR_ENGINE`: the user approved the plan; execution remains
  blocked because there is no device-action executor in this version.
- `RUNNING`, `VERIFYING`, `COMPLETED`, `FAILED`, `STOPPED`: reserved
  vocabulary for a future executor. Current UI flows cannot write these states.

Every review transition records a timestamped history event. Approving a plan
must never be confused with performing the task or verifying its outcome.

## Modules

- `data/AgentTask.kt`: task, step, event models and review-transition rules.
- `data/TaskPlanFactory.kt`: transparent deterministic starter checklist.
- `data/TaskStore.kt`: local persistence and one-time migration from earlier
  draft records; contains no network client or device-action API.
- `data/LocalCompanion.kt`: rule-based offline conversational fallback.
- `ui/PhoneAgentApp.kt`: companion chat, activity cards, review actions and
  truthful safety disclosure.
- `.github/workflows/android-ci.yml`: unit tests, debug APK build, artifact upload.

## Safety requirements before actual execution

Before any tool can run a real action, a separate controller must validate its
schema, allowlisted capability, current permission, user approval, risk policy,
and cancellation state. Every action needs a tested success path, denied-
permission path, failure path and cancellation behavior.

The eventual completion state must only be written after result evidence is
observed or the user explicitly confirms the result. Sending a tap is not proof
that an action succeeded.

## Planned capability strategy

1. Add safe Android intents and Sharesheet intake before broad accessibility.
2. Introduce a typed capability registry; the LLM cannot invoke tools directly.
3. Keep model choice and external data transfer explicit and opt-in.
4. Add Accessibility only as a separately enabled fallback, with clear status,
   scoped app access and live-device tests.
5. Use durable structured storage such as Room once task volume and migration
   needs justify it.

## Build

- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.2.21 + Compose compiler plugin
- JDK 17, Android SDK 36, minimum API 26

The workflow generates a debug APK. A production release is not signed by this
repository.
