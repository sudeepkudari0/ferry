# Third-Party Notices

This project contains none of the source code listed below — it depends on
them either as a separately-installed runtime dependency or as a library
pulled in via Gradle. Listed here for transparency and attribution.

## Runtime dependency (not bundled, not linked — a separate installed app)

- **Mobilerun Portal** — https://github.com/droidrun/mobilerun-portal
  License: MIT
  This app requires Portal to be installed and its Accessibility Service
  enabled by the user. This app communicates with Portal exclusively over
  its local HTTP/WebSocket API on-device; no Portal source is copied,
  embedded, or redistributed by this project.

## Library dependencies (via Gradle)

| Library | License |
|---|---|
| AndroidX (AppCompat, ConstraintLayout, Lifecycle, WorkManager, Security-Crypto) | Apache License 2.0 |
| Material Components for Android | Apache License 2.0 |
| OkHttp | Apache License 2.0 |
| Retrofit | Apache License 2.0 |
| Gson | Apache License 2.0 |
| JUnit4 | Eclipse Public License 1.0 |
| Mockito | MIT License |

If you add further dependencies, add them to this table in the same PR.
