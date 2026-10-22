# Cloud Agent Engine Foundation

**Status:** Implementation contract for the next PhoneAgent milestone.
**Scope:** Architecture and acceptance criteria only; this document does not claim the engine is implemented.

## Objective

Move PhoneAgent from locally stored task drafts to a durable, resumable agent workflow inspired by the observable behavior of cloud task agents. Reimplement the product behavior; do not copy proprietary Claude/Anthropic source code.

## Required components

1. **Android client** — task creation, plan review, approval, progress, stop/take-over, and evidence display.
2. **Task API** — authenticated endpoints to create tasks, read task state, approve a plan, request cancellation, and read events.
3. **Durable task store** — persists task state and event history independently of the mobile process.
4. **Worker / scheduler** — claims approved tasks, resumes interrupted work, enforces timeouts and retry limits, and emits progress events.
5. **Planner adapter** — converts a user request into schema-validated steps. It must not execute tools directly.
6. **Policy gate** — checks tool allowlists, risk level, user approval, scope, and cancellation before every consequential action.
7. **Tool registry** — explicit contracts for each supported integration, including inputs, permission requirements, risk class, timeout, and verification procedure.
8. **Verifier** — marks a step complete only when a tool returns a verifiable result; otherwise uses attempted, blocked, or awaiting-confirmation states.
9. **Audit journal** — append-only events for task creation, plan revisions, approvals, tool attempts, observed results, cancellation, and errors. Never log secrets.

## Task lifecycle

`DRAFT -> PLANNING -> NEEDS_APPROVAL -> APPROVED -> QUEUED -> RUNNING -> VERIFYING -> COMPLETED`

Additional terminal or waiting states: `NEEDS_INPUT`, `BLOCKED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`.

Rules:
- A plan approval is not permission for every future action; each tool call is checked against policy.
- High-impact actions (send, delete, purchase, publish, financial, account/security changes) require explicit, scoped approval.
- Cancellation prevents new steps from starting. The system reports if an in-flight external action cannot be reversed.
- Retries are bounded, idempotency keys are used where possible, and a worker crash must not silently duplicate a consequential action.
- The UI may show "completed" only when the verifier records evidence or the user explicitly confirms the outcome.
- Never pretend that an unavailable tool ran.

## Proactive operation

Scheduled checks must be opt-in, scoped to explicitly connected sources, rate-limited, and visible in settings. The scheduler should notify the user only for a meaningful change, a completed task, a blocker, or a decision requiring approval. No hidden scraping or undisclosed data transfer.

## Security and privacy

- Use server-side secrets only; never embed model or backend service keys in the APK or repository.
- Authenticate each client and authorize access per user/task.
- Encrypt transport; minimize stored personal data; define retention and deletion behavior before production.
- Disclose the model provider, data leaving the device, and expected costs before first use.
- Local-only mode must not make network calls.
- Android actions remain a separate local executor using supported intents first; Accessibility is optional, manually enabled, and capability-scoped.
- Never bypass Android secure-screen, permission, or operating-system protections.

## Implementation sequence

### Phase A — stable task contract
- Add explicit task, step, approval, and event schemas.
- Define legal state transitions in a pure state machine.
- Add tests for forbidden transitions, cancellation, duplicate events, and completion verification.
- Preserve legacy local drafts through migration.

### Phase B — cloud persistence and API
- Select a backend only after confirming deployment and secret-management availability.
- Add authentication, task CRUD, approval/cancel endpoints, and event retrieval.
- Add API contract tests and verify restart persistence.

### Phase C — durable worker
- Queue approved tasks, lease work safely, emit events, enforce timeouts/retry limits, and resume after worker restarts.
- Start with a harmless dry-run tool and prove idempotency/cancellation before any real action.

### Phase D — model adapter and tools
- Add a provider-neutral model interface and strict structured-output validation.
- Add one low-risk integration at a time with success, denial, timeout, cancellation, and verification tests.

### Phase E — Android integration
- Connect the app to the task API; display live status and event history.
- Add the first native-intent action only after API and policy tests pass.
- Test on a physical Android device before claiming the action works.

## Release gates

- Existing package identity and signing strategy are resolved before distributing another APK.
- Unit tests and API/worker integration tests pass.
- No secrets in repository or APK.
- No false completion state in tests.
- Data migration, cancellation, and duplicate-execution behavior are documented.
- A test task is observed end-to-end before enabling real-world actions.

## Current known gap

The current app has a local SharedPreferences task store and deterministic plan templates. It has no cloud API, durable worker, live LLM, or device-action executor. This milestone must close those gaps incrementally and must not describe the current app as an autonomous cloud agent.
