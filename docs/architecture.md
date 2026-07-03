# Architecture & Design Decisions

## Why native Java/Android Studio, not React Native/Expo/Flutter

`android.accessibilityservice.AccessibilityService` is a system class that
must be declared in the manifest and implemented natively — there's no JS/Dart
equivalent. Any cross-platform framework would still require this exact piece
written natively and bridged in, paying a bridge-latency tax on every
observe/act step for zero real cross-platform benefit, since iOS has no
equivalent automation surface for non-jailbroken apps. Native Java avoids the
bridge entirely and matches Portal's own stack closely enough that extending
it later, if ever needed, requires no context switch.

## Why depend on Mobilerun Portal instead of building an accessibility engine

Portal's accessibility engine (tree extraction, gesture dispatch, screenshot
capture) is ~44k lines of tested, MIT-licensed Kotlin. Reimplementing it would
duplicate solved work without adding anything to the part of this project
that's actually differentiated — the agent loop, provider abstraction, and
product UX. See [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) for
how the dependency boundary is kept clean (local API client only, no source
copied).

## Why an LlmProvider interface instead of calling Claude directly everywhere

`AgentLoop` only depends on the `LlmProvider` interface, never a concrete
provider. This is what makes BYOK-across-providers and a future local-model
mode a one-line change at the call site, and it's what makes `AgentLoop`
unit-testable with a scripted fake instead of hitting a real API in tests
(see `app/src/test`).

## Why a foreground Service instead of running the loop on the Activity

Android will suspend/kill background work without a foreground service, and
a task that spans multiple app-switches will very likely outlive the
Activity's lifecycle (screen rotation, user switching apps to check
something). The persistent notification is also a deliberate UX/trust
decision, not just a technical requirement — the user should always be able
to see that an agent is actively acting on their behalf and be able to stop
it.

## Why the accessibility tree is the primary signal, not screenshots

Structured node data (resource IDs, text, content descriptions) is cheaper,
faster, and more reliable to act on than vision — coordinates drift across
screen sizes and themes, text/IDs generally don't. Screenshots are kept as an
explicit fallback path (`fetchStateWithScreenshot`) for screens where the
tree is unusable (canvas-drawn UI, some WebViews), not the default per-step
signal.

## Open items / things intentionally left as TODOs

- `PortalClient`'s endpoint paths and JSON shapes are placeholders pending
  verification against Portal's actual API — do not treat them as
  authoritative until confirmed against a running instance.
- The local auth token handshake with Portal isn't wired up yet — needs
  Portal's real pairing flow.
