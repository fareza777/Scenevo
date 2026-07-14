# Scenevo Architecture

## Why this stack (production-realistic)

OpenMontage is a **desktop agent + Remotion/FFmpeg** pipeline. On Android Play Store we cannot ship Remotion/Node. The closest stable production path:

| Concern | Choice | Why |
|---|---|---|
| UI | Jetpack Compose | Play Store default, maintainable |
| Structure | Multi-module Clean Arch | Feature isolation, faster CI, clear boundaries |
| Persistence | Room | Offline projects/timelines without a backend |
| Secrets | EncryptedSharedPreferences | BYO API keys never in plaintext |
| Preview | Media3 ExoPlayer | Google-supported, hardware decode |
| Export | Media3 Transformer | Official on-device composition/export |
| Transitions | `TransitionEffectFactory` + `FfmpegTransitionBridge` | Media3 effects now; FFmpeg polish pluggable |
| Default voice | Android TTS | Zero download, offline |
| Neural voice | Piper pack + PAD / sherpa engine | Optional on-device |
| Stock | Pexels (consent + Wi‑Fi) | Cached under `files/stock_cache` |
| State | ViewModel + Flow | Lifecycle-safe, testable |
| DI | Hilt | Standard for multi-module Android |

## Data flow

```
UI (feature/*)
  → ViewModel
    → UseCase / Engine
      → Repository (domain interface)
        → Room / DataStore / Files
```

Render path:

```
Project → TimelineComposer → Media3VideoRenderer
                              ├─ TransitionEffectFactory (bumpers + RGB/scale)
                              └─ FfmpegTransitionBridge (optional post-polish)
```

## Privacy model

- Core features never require network.
- `INTERNET` + `ACCESS_NETWORK_STATE` exist for optional BYO AI, Pexels stock, and Piper pack download.
- Stock requires explicit consent + default Wi‑Fi-only gate.
- Cloud backup of DB + secrets disabled via backup rules.
- Cleartext network limited to loopback (Ollama).

## Package map

- `domain.model` — serializable project graph (scenes, voice, music, export)
- `engine.timeline` — edit director equivalent (deterministic)
- `engine.render` — compose director equivalent (on-device)
- `engine.tts` — narration providers
- `feature.create` — wizard UX matching “naskah → aset → suara”

## Non-goals (v1)

- Mandatory accounts / subscriptions
- Server-side render credits
- Bundled paid generative video APIs
- Automatic cloud upload of user footage
