# Handoff: Mobile Agent — continue from initial scaffold

## Context (read this before doing anything)

This is a native Android app (Java, minSdk 26) that lets an LLM automate
on-screen tasks via Mobilerun Portal (https://github.com/droidrun/mobilerun-portal,
MIT licensed, NOT vendored into this repo — treated as a separate installed
runtime dependency, communicated with only over its local HTTP/WebSocket API).

The current commit is a scaffold: correct architecture and file boundaries,
but two things are explicitly **placeholder and unverified**:
1. `app/src/main/java/.../net/PortalClient.java` — endpoint paths (`/state`,
   `/action`), JSON request/response shapes, and the local auth-token
   handshake are inferred, not confirmed against Portal's real API.
2. App icon is a placeholder vector.

Everything else (Action model, LlmProvider/ClaudeProvider, AgentLoop,
SecureKeyStore, AgentTaskService, the UI, the unit tests) is real, working
code, not stubs — read it before changing it rather than assuming it needs
to be rebuilt. `docs/architecture.md` explains every major design decision
and why alternatives (React Native, building an accessibility engine from
scratch, calling providers directly without an interface) were rejected —
read it before proposing architecture changes.

## Ground rules for this work

- `AgentLoop` must stay free of any provider-specific or transport-specific
  code — it only depends on `LlmProvider` and `PortalApi`, never a concrete
  implementation. Don't break this boundary for convenience.
- Never hardcode an API key anywhere. BYOK only, via `SecureKeyStore`.
- Don't vendor/copy Portal's source into this repo. It stays an external
  dependency, communicated with only via its local API. Any exception to
  this needs to be flagged to the human, not decided unilaterally.
- Every new dependency gets added to `THIRD_PARTY_NOTICES.md` in the same
  commit/PR that introduces it.
- Run `./gradlew testDebugUnitTest lintDebug assembleDebug` before
  considering any phase below done — don't report a phase complete on
  code you haven't actually built/tested.
- Commit in small, reviewable chunks per phase below, not one giant commit.

## Phase 1 — Make the core actually work end to end

1. Clone and run Mobilerun Portal on a real or emulated Android device.
   Determine its real local API contract (check its README/docs, any
   OpenAPI spec, or inspect requests/responses directly with curl/Postman
   against the running instance).
2. Update `PortalClient.java` to match the real contract — endpoint paths,
   JSON shapes, and the local auth-token pairing/handshake mechanism.
   Update `DeviceState.java`'s fields to match whatever Portal actually
   returns.
3. Update `docs/architecture.md`'s "Open items" section to mark this
   resolved, and remove the now-stale TODO comments from the code.
4. Manually verify one full task end to end on a real device (start with
   something trivial like "open Settings and check battery," matching the
   README's own roadmap suggestion) before moving on.
5. Add integration coverage for `PortalClient` itself (not just `AgentLoop`
   against fakes) — e.g. a test that hits a local mock server
   (`okhttp3.mockwebserver` is already a test dependency) verifying request
   shape and response parsing.

## Phase 2 — Bring the repo up to industry-standard OSS hygiene

Add, in this order:

1. `CODE_OF_CONDUCT.md` — Contributor Covenant is the standard default.
2. GitHub issue templates (`.github/ISSUE_TEMPLATE/bug_report.md`,
   `feature_request.md`) and a PR template
   (`.github/pull_request_template.md`) referencing the checklist already
   in `CONTRIBUTING.md`.
3. `CHANGELOG.md` following Keep a Changelog format, starting from this
   initial scaffold as `0.1.0`.
4. Semantic versioning going forward — tag releases accordingly, and wire
   `versionName`/`versionCode` in `app/build.gradle` to match tags.
5. A `SECURITY.md` — given this app requests real automation access and
   handles a user's API keys, add a clear responsible-disclosure policy
   and contact method, even if it's just "open a private security advisory
   on GitHub."
6. Repo topics/description on GitHub itself (not a file, but do it):
   android, accessibility-service, llm-agent, automation, byok.
7. README polish once Phase 1 is done: replace the architecture ASCII
   diagram with an actual rendered diagram if convenient, add a short demo
   GIF/video once there's a working flow to show, add badges (CI status,
   license, latest release).
8. Static analysis: add Checkstyle or Spotless (ask the human which they
   prefer before picking) and wire it into the existing CI workflow
   (`.github/workflows/android-ci.yml`) as an additional step.

## Phase 3 — Ship the one flow the README commits to

The README's roadmap explicitly commits to ONE fully reliable end-to-end
flow (email summarization) before broadening scope. Do not start building
additional flows (job applications, form filling, etc.) until this one is
demonstrably reliable across repeated runs. Add a short section to the
README once done: what the flow does, its limitations, and a demo
GIF/video link.

## Phase 4 — Only after Phase 3 is solid

- In-app step history/trajectory viewer (currently notification-only —
  `AgentLoop.StepListener` already gives you every hook needed for this,
  no architecture change required).
- Additional BYOK providers (OpenAI, Gemini, OpenRouter) — implement each
  as a new `LlmProvider`, following `ClaudeProvider` as the reference
  pattern. Do not modify `AgentLoop` or `LlmProvider` to accommodate
  provider quirks — if a provider genuinely doesn't fit the interface,
  flag that to the human rather than widening the interface unilaterally.
- Vision fallback path (`fetchStateWithScreenshot` already exists in
  `PortalClient` as a placeholder — wire it to an actual vision-capable
  call path, gated so it's not used by default per step).
- Local model support — this is a bigger design conversation (which
  on-device inference framework, which model, quality tradeoffs are
  already discussed in prior project history) — propose an approach and
  confirm with the human before implementing, don't just pick one.

## What to report back after each phase

For each phase: what changed, what you verified (with command output, not
just "should work"), and anything you deliberately left as a TODO with
reasoning — don't silently skip something the phase asked for.
