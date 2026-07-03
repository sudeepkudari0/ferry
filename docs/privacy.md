# Privacy

This project has no backend. There is no server operated by this project
that your data passes through. The only network calls this app makes are:

1. **To your chosen LLM provider's API** (e.g. `api.anthropic.com`), using
   the API key you provide (BYOK). Sent per agent step:
   - Your task description (what you typed)
   - The current screen's accessibility tree content — this can include
     text visible on screen at that moment (email subject lines, names,
     etc., depending on what app is open when a step runs)
   - A history of the actions taken so far this task
   - A screenshot, only on steps where the vision fallback is explicitly
     triggered (not by default)

2. **To Portal, over `127.0.0.1` (local loopback only)** — this never
   leaves the device.

## What is stored on-device

- Your LLM provider API key, encrypted via Android Keystore
  (`EncryptedSharedPreferences`) — never logged, never included in crash
  reports, never sent anywhere except as the Authorization header to that
  provider's own API.
- Task history, if/when the in-app history view ships (see roadmap) — kept
  local to the device.

## What this means practically

Because the accessibility tree is sent to your LLM provider as part of
normal operation, avoid running tasks on screens showing information you
would not want included in that provider's API request (e.g. don't run a
task while a password field or highly sensitive document is on-screen).
This is inherent to how the agent decides what to do next, not a bug — if
you need stricter guarantees, wait for local-model support (tracked on the
roadmap) or scope tasks away from screens with sensitive on-screen content.
