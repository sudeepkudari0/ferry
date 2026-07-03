# Contributing

Thanks for considering it — this is an early-stage personal project, so
process is intentionally light.

## Before opening a PR

- Run `./gradlew testDebugUnitTest lintDebug` locally and make sure both pass
  (CI runs the same checks on every PR).
- Keep `AgentLoop` free of any provider-specific or transport-specific code —
  it should only ever depend on the `LlmProvider` and `PortalApi` interfaces.
- If you add a dependency, add it to `THIRD_PARTY_NOTICES.md` in the same PR.
- If you touch `PortalClient`'s endpoint paths/payloads, note in the PR
  description what you verified them against.

## Reporting issues

Open a GitHub issue. For anything touching the privacy/data-flow behavior
described in `docs/privacy.md`, please flag it explicitly in the issue title.

## Code style

Standard Java conventions, 4-space indentation. No formatter is enforced yet
(open to adding one — Spotless/Checkstyle — via issue/PR if you have a
preference).
