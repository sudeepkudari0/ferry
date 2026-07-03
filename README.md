# Mobile Agent

An on-device Android app that lets an LLM carry out natural-language tasks
by reading and acting on your screen — "open Gmail and summarize my unread
messages," "find this job listing and open the application form," and
similar multi-step, cross-app workflows.

> Status: early / actively developed. The first working slice targets a
> single reliable flow (see [Roadmap](#roadmap)) before expanding scope.

## Why this exists

Android has no equivalent of browser automation (Playwright/Selenium) for
general apps. This project uses Android's Accessibility Service — the same
mechanism screen readers use — to read the on-screen UI structure and act on
it, orchestrated by an LLM in a plan → observe → act loop, running entirely
on the phone.

## Architecture

```
┌─────────────────────────────┐        ┌──────────────────────────────┐
│   Mobile Agent (this repo)  │        │  Mobilerun Portal (separate,  │
│   - UI (task input/status)  │  local │  user-installed dependency)   │
│   - AgentLoop (orchestrator)│◄──────►│  - AccessibilityService       │
│   - LlmProvider (BYOK/local)│  HTTP/ │  - reads UI tree, dispatches  │
│   - PortalClient            │   WS   │    taps/swipes/text input     │
└─────────────────────────────┘        └──────────────────────────────┘
              │
              │ HTTPS (user's own API key)
              ▼
    ┌───────────────────┐
    │  LLM provider API  │   e.g. Anthropic — decides the next action
    │  (BYOK)             │   given task + current screen state
    └───────────────────┘
```

This app **depends on** [Mobilerun Portal](https://github.com/droidrun/mobilerun-portal)
(MIT licensed) for the accessibility engine, and talks to it over its local
API. No Portal source is included in this repo — see
[THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md). See
[docs/architecture.md](./docs/architecture.md) for the full design writeup,
including why this approach was chosen over React Native/Flutter and over
building an accessibility engine from scratch.

## Requirements

- Android 8.0 (API 26) or higher
- [Mobilerun Portal](https://github.com/droidrun/mobilerun-portal) installed, with its Accessibility
  Service enabled in system settings
- An API key from a supported LLM provider (BYOK — see [Privacy](#privacy--byok))

## Setup (development)

```bash
git clone https://github.com/<your-username>/mobile-agent.git
cd mobile-agent
# Open in Android Studio (Koala or newer), let it sync Gradle,
# or build from the CLI:
./gradlew assembleDebug
```

1. Install and enable Portal on your test device (see its own README for
   pairing/setup — this is a hard prerequisite, the app will warn you if it's
   missing).
2. Install this app, open **Settings**, and enter your Anthropic API key.
3. Type a task and hit **Run task**.

## Privacy / BYOK

There is no backend server for this project. Your API key is stored
encrypted on-device (Android Keystore-backed) and used only to call your
chosen LLM provider's API directly from your phone. Screen content read via
Portal is sent to that same provider as part of deciding each action — see
[docs/privacy.md](./docs/privacy.md) for exactly what leaves the device and
when.

## Roadmap

- [x] Project scaffolding, Portal client, agent loop, BYOK (Claude)
- [ ] Verify and finalize Portal's real API contract (current endpoints in
      `PortalClient` are placeholders — see inline `TODO`s)
- [ ] Ship one fully reliable end-to-end flow (email summarization) before
      broadening scope
- [ ] In-app step history / trajectory viewer (currently notification-only)
- [ ] Additional BYOK providers (OpenAI, Gemini, OpenRouter)
- [ ] Optional local model support for simple/offline tasks
- [ ] Vision fallback path for screens with unusable accessibility trees

## Known limitations

- Sideload-only for now (not distributed via Play Store) — see
  [docs/architecture.md](./docs/architecture.md) for why.
- Some apps mark fields as `accessibilityDataSensitive` (Android 16+),
  which will make them unreadable to this approach by design.
- Automating account actions on some platforms (e.g. bulk job applications)
  may conflict with that platform's own terms of service — this is a
  personal-use tool, use it accordingly.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

This project's own code is MIT licensed — see [LICENSE](./LICENSE).
Third-party dependencies are listed in
[THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md).
