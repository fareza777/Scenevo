# Scenevo

**Offline Video Maker** for Android — local-first montage like [OpenMontage](https://github.com/calesthio/OpenMontage), built for Play Store quality: script → scenes → visuals → voice → timeline → on-device MP4.

## Principles

| Principle | How Scenevo honors it |
|---|---|
| Local-first | Projects, assets, timelines in Room + app storage |
| Privacy-first | No account required; API keys encrypted on device |
| Offline-capable | Script split, TTS, preview, render work without network |
| No forced subscription | Core montage + export is free of render credits |
| On-device render | Media3 Transformer (FFmpeg path pluggable later) |
| Optional AI | BYO OpenAI / Anthropic / Gemini key or Ollama local |
| No expensive backend | Zero required server |

## Stack

- **Language:** Kotlin 2.0
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture, multi-module
- **DI:** Hilt
- **State:** ViewModel + Kotlin Flow
- **DB:** Room
- **Settings / secrets:** DataStore + EncryptedSharedPreferences
- **Preview / export:** Media3 ExoPlayer + Media3 Transformer
- **TTS (default):** Android TextToSpeech (offline)
- **Min SDK:** 26 · **Target / Compile:** 35

## Modules

```
app                  → Application, navigation
core/common          → Result, dispatchers
core/designsystem    → Theme + shared Compose components
core/database        → Room
core/datastore       → Preferences + encrypted API keys
domain               → Models, repository contracts, use cases
data                 → Repository implementations + Hilt wiring
engine/timeline      → Deterministic timeline composer
engine/render        → On-device Media3 renderer
engine/tts           → Narration engines
feature/*            → Home, Create wizard, Editor, Export, Settings
```

## Pipeline (OpenMontage-inspired, mobile)

1. **Script** — user pastes naskah  
2. **Scenes** — local splitter (blank lines / sentences → timed scenes)  
3. **Visuals** — gallery / files assigned to scenes (Ken Burns + transitions)  
4. **Voice** — offline Android TTS (Piper / BYO cloud TTS later)  
5. **Edit** — timeline review  
6. **Compose** — on-device MP4 export  

Optional AI (settings) can later assist scene planning / asset tagging — never required for export.

## Build

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

Open in Android Studio (Ladybug+), sync, run on device/emulator API 26+.

`local.properties` must contain your SDK path (already set if cloned on this machine):

```
sdk.dir=C:\\Android\\Sdk
```

## Roadmap toward Play Store

- [x] Multi-module scaffold + local project CRUD  
- [x] Script → scenes wizard  
- [x] Asset attach + timeline model  
- [x] Offline TTS hook  
- [x] On-device Media3 export path  
- [x] Optional AI settings (BYO key / Ollama)  
- [x] Rich timeline scrubber + Media3 preview player  
- [x] Burn-in subtitles + SRT sidecar  
- [x] Music attach + volume ducking  
- [x] Persistable gallery URIs + MediaStore publish / share  
- [x] Project delete + duplicate  
- [x] Editor scene reorder / duration / transitions  
- [x] Export resolution presets (720p / 1080p / 4K)  
- [ ] FFmpeg advanced transitions (behind `VideoRenderer`)  
- [ ] Piper ONNX local neural TTS pack  
- [ ] Stock cache (Pexels) with explicit user consent + Wi‑Fi only  
- [ ] Play Asset Delivery for optional voice packs  

## License

Apache-2.0 (intended). Confirm before publishing.
