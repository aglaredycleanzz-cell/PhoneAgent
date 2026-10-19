# Product Strategy: Phone Agent

**Status:** Working product thesis, 2026-10  
**Purpose:** Focus the project on a real market need instead of competing on avatars or generic AI chat.

## 1. Executive decision

Do not position Phone Agent as "another AI that can tap your phone." That category already has Google Gemini task automation, Android-first open-source agents, and mature rule-based automation tools.

### Proposed positioning

> **Phone Agent is an Arabic-first, trust-first workspace for getting phone tasks done — with a visible plan, user-controlled permissions, intervention at any point, and evidence of what actually completed.**

The working name remains **Phone Agent** in this repository. Branding and final naming should be validated with users before a public launch.

The differentiating product idea is not the avatar alone. It is the combination of:

1. **Arabic-first task dialogue:** Modern Standard Arabic and common dialect input, with English technical words/code mixed into the same request.
2. **A functional companion team:** NOVA plans, ORION handles technical reasoning, and SENTINEL checks risk. These are transparent roles in one workflow, not merely three cosmetic chatbots.
3. **A task record users can trust:** Every requested action becomes a structured card with intent, plan, required capability, approval state, live progress, outcome, and evidence. The app must never say a task succeeded just because it sent a tap.
4. **Explicit control:** A clear stop/take-over path, risk-based confirmation, app allowlists, and a visible activity history.
5. **A focused first market:** Arabic-speaking Android power users, freelancers, and micro-business operators who repeat administrative workflows — not every possible phone user on day one.

No claim of being unique is established by desk research alone. The hypothesis must be tested with real users and current competitors.

## 2. Competitive landscape

This is a feature comparison from public product documentation and repositories, not a verified hands-on benchmark.

| Alternative | What it already does well | Implication for Phone Agent |
|---|---|---|
| **Google Gemini task automation** | Multi-step automation in selected apps, background progress, visibility through notifications, and controls to view, jump in, or stop. Availability is device/app limited according to Google's announcement. | Background execution and progress visibility alone are not enough to differentiate. |
| **ClosePaw** | Android-first open source, floating Smart Capsule, pause/take-over, provider choice, per-app approval, and hard blocks for sensitive apps. Optional background virtual display requires Shizuku. | We must not present an overlay or safety controls as a novel idea by themselves. |
| **PalmClaw** | On-phone sessions, context, memory, skills, structured tools, external integrations, and explicit execution boundaries. It requires a configured model provider. | Persistent context and a tool framework are table stakes in this segment. |
| **OpenDroid** | Self-planning, sequential task execution, routines, and re-evaluation after failures, as described by its repository. | Planning and retry behavior alone are not defensible differentiation. |
| **Android Agent (Fox-Islam)** | No PC/ADB/root in its described setup, bring-your-own compatible model endpoint, Accessibility-based screen interaction. | Basic no-PC control and bring-your-own-model support are already represented. |
| **Tasker** | Mature context-driven automation: profiles connect conditions such as app, time, location, state, or events to sets of actions. | Phone Agent should complement visual rule automation with natural-language task creation, not rebuild every trigger/action rule editor. |

### Sources to recheck before release

- Gemini multi-step tasks: https://blog.google/innovation-and-ai/products/gemini-app/android-multi-step-tasks/
- ClosePaw: https://github.com/imoonkey/closepaw
- PalmClaw: https://github.com/ModalityDance/PalmClaw
- OpenDroid: https://github.com/yashab-cyber/opendroid
- Android Agent: https://github.com/Fox-Islam/android-agent
- Tasker guide: https://tasker.joaoapps.com/userguide_summary.html

Competitor capabilities, availability, licensing, and repository activity change. Revalidate these sources before making public comparison claims. Do not reuse competitor code or assets without a separate license and attribution review.

## 3. Initial customer and problem

### Primary segment

Arabic-speaking Android users who repeatedly move information between conversations, notes, reminders, and work apps — particularly freelancers and micro-business operators.

### Core job to be done

"When I receive a messy request or have a multi-step task, help me turn it into an actionable plan, carry out only the steps I approve, and show me exactly what happened."

### Starting scenarios

Choose three narrow scenarios for an MVP:

1. **Capture and structure:** Share text from another app into Phone Agent; convert it into a titled task with steps, due date if stated, and open questions.
2. **Safe device actions:** Open an approved app or launch a built-in Android intent, with a visible preview before the action.
3. **Repeatable routine:** Save a proven workflow as a reusable template, with user-set permissions and a clear "run once" control.

Do not promise arbitrary, reliable control of every app. App interfaces differ, Android restrictions vary by manufacturer, and accessibility actions need live-device testing.

## 4. The signature experience

### One conversation; three useful roles

- **NOVA — Planner:** clarifies intent, breaks the request into steps, and asks only for genuinely missing information.
- **ORION — Operator:** selects an allowed tool for each step and reports its real result. In versions without the tool, it says the capability is unavailable rather than pretending.
- **SENTINEL — Verifier:** checks the risk level, requests approval where required, and verifies completion using available evidence.

Users can talk to any avatar, but the app should show when a role hands work to another. Switching the avatar must not create a separate silo of task history; the task record remains shared while conversations can retain individual tone/preferences.

### Task cards, not a dead-end chat

Each actionable request should have a durable card with:

- original request and parsed intent;
- proposed steps and success criteria;
- capabilities/apps required and current permission state;
- approval state and a concise explanation of risk;
- live state: Draft → Needs approval → Running → Verifying → Completed / Failed / Stopped;
- evidence, error message, and recovery option;
- pause, stop, and user take-over controls while an operation is active.

**Proof-of-completion rule:** only mark a task complete when the relevant result has been observed or explicitly confirmed by the user. Otherwise report "attempted", "awaiting confirmation", or "unable to verify".

## 5. Trust model as a product feature

The trust model should be visible and understandable, not buried in a settings page.

- **Ask every time:** default for sending messages, deleting/editing user data, purchases, publishing, or other consequential actions.
- **Allow for this session:** temporary permission for a defined app/capability.
- **Always allow:** only for low-risk, narrowly scoped capabilities, with an easy way to revoke.
- **Blocked by default:** banking, password managers, authenticator apps, crypto-wallets, and any sensitive app unless there is a separately justified and safe integration.
- **Local-only mode:** no conversation sent to an external model; explain that this mode may have more limited reasoning.
- **Provider mode:** model provider, data transmitted, and any expected costs disclosed before the first request.
- **Audit trail:** record what was requested, approved, attempted, and verified; let users clear it.
- **Stop means stop:** cancellation must prevent new actions from being queued. Explain when an action already in progress cannot be reversed.

These are product requirements, not capabilities already implemented in version 0.1.

## 6. Technical strategy

Prefer deterministic integrations and Android intents where possible. Use Accessibility as a carefully bounded fallback, not as the only integration strategy.

1. **Share entry point:** Android Sharesheet and typed imports to turn selected content into a task. Explicitly disclose if shared text/images are stored or sent to a model.
2. **Structured capability registry:** every tool declares input schema, risk class, required permission, supported Android versions, and test procedure.
3. **Policy gate:** the planner cannot invoke tools directly. An execution controller validates the schema, allowlist, permission, approval, and cancellation state.
4. **Execution journal:** record request, step, outcome, timestamp, and evidence. Avoid logging credentials or unnecessary personal content.
5. **Native integrations first:** use intents/public APIs for launching apps, composing messages, creating calendar/reminder drafts, and sharing content where supported.
6. **Accessibility fallback:** optional service enabled manually by the user, with a persistent status and clear explanation of screen access. Do not request or enable it before the service and safety gates are implemented.
7. **Optional model adapter:** keep model credentials out of source control. Start with user-managed provider settings rather than bundling paid API usage. Do not claim fully local intelligence until an on-device model is integrated and tested.

Android may limit background execution, overlays, and access to secure or protected screens. The product must communicate such limits rather than attempt to bypass them.

## 7. Monetization hypothesis

Keep the base app open source and useful with a low setup burden. Test a model-provider-neutral approach before investing in an expensive hosted service.

Potential future revenue options, subject to user validation:
- optional paid workflow packs for defined professions or local business routines;
- managed cloud sync/backup only for users who explicitly opt in;
- paid premium integrations/support for teams or small businesses;
- a bring-your-own-model option so API charges are visible to the user.

Do not monetize through selling conversation data or undisclosed telemetry. Do not build subscriptions before users return to complete recurring tasks.

## 8. MVP roadmap

### M0 — Product shell (current)
Arabic chat interface, three distinct visual companions, local transcript storage, activity tab, honest capability disclosure. The replies are currently rule-based, not a real LLM.

### M1 — Make tasks useful without broad permissions
Implement a structured task model and task cards. Add Android Sharesheet intake, task editing, step list, status, and clear "not executable yet" state. No Accessibility requirement yet.

### M2 — First controlled device actions
Implement only a small allowlist of testable actions through explicit Android intents. Add confirmation, cancellation, result/error states, and unit tests. Demonstrate each action on a physical device before claiming support.

### M3 — Optional intelligence
Add a user-configured model endpoint and structured planning output. Make provider, data transfer, and cost visible. Validate model output against schemas; it cannot directly generate executable shell commands or arbitrary clicks.

### M4 — Accessibility execution
Add a separately declared AccessibilityService, opt-in setup screen, app/capability allowlist, risk policy, progress notification, interruption, action log, and physical-device testing. Avoid a blanket promise to control every app.

### M5 — Market validation
Recruit 10–15 target users for discovery interviews; then test a prototype with 5–10 users over one week. Do not make market-size claims until there is credible research.

## 9. Success criteria

Track user value and trust before growth:

- task completion rate, with "verified" separated from "attempted";
- percentage of tasks completed without avoidable clarification;
- user stops, overrides, and recovery success;
- time saved versus manual workflow;
- repeated use of a saved routine within 7 days;
- permission opt-in rate and permission revocations;
- zero silent external transfers and zero falsely reported completions in the test suite.

Initial internal release gate: every shipped action must have a happy-path test, denial-path test, cancellation test, and documented limitation.

## 10. Immediate priority

**Build the task card and execution journal before connecting a model or adding broad Accessibility control.** This creates the product's distinctive contract: the user can see the intention, approve the capability, interrupt the work, and inspect what actually happened.

Once that record works, model providers and device actions become replaceable modules rather than the product itself.
