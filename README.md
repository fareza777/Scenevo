# Scenevo

**Offline Video Maker** for Android — local-first montage like [OpenMontage](https://github.com/calesthio/OpenMontage), built for Play Store quality: script → scenes → visuals → voice → timeline → on-device MP4.

## Principles

| Principle | How Scenevo honors it |
|---|---|
| Local-first | Projects, assets, timelines in Room + app storage |
| Privacy-first | No account required; API keys encrypted on device |
| Offline-capable | Script split, TTS, preview, render work without network |
| No forced subscription | Core montage + export is free of render credits |
| On-device render | Media3 Transformer + pluggable FFmpeg polish bridge |
| Optional AI | BYO OpenAI / Anthropic / Gemini key or Ollama local |
| Optional stock | Pexels with explicit consent + Wi‑Fi-only policy |
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
- **TTS (optional):** Piper voice pack + sherpa/Piper system engine / PAD
- **Stock (optional):** Pexels BYO key, cached locally
- **Min SDK:** 26 · **Target / Compile:** 35

## Modules

```
app                  → Application, navigation, PAD asset packs
core/*               → common, designsystem, database, datastore
domain / data        → models + repositories
engine/timeline      → Deterministic timeline composer
engine/render        → Media3 renderer + transition bridge
engine/tts           → Android TTS + Piper packs + PAD delivery
engine/stock         → Pexels search + on-device cache
feature/*            → Home, Create, Editor, Export, Settings
assetpacks/piper_voices → Play Asset Delivery on-demand voice pack
```

## Pipeline

1. **Script** — paste naskah  
2. **Scenes** — local splitter  
3. **Visuals** — gallery and/or Pexels stock (consent)  
4. **Voice** — Android TTS / Piper preference  
5. **Music** — optional ducked bed  
6. **Edit** — preview, reorder, transitions, duration  
7. **Export** — on-device MP4 + SRT → Movies/Scenevo  

## Build

```bash
.\gradlew.bat :app:assembleDebug
```

`local.properties` must contain your SDK path:

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
- [x] Advanced transitions (`TransitionEffectFactory` + `FfmpegTransitionBridge`)  
- [x] Piper ONNX voice pack download + prefer-local TTS  
- [x] Stock cache (Pexels photos default; videos optional BYOK) + consent + Wi‑Fi only  
- [x] Optional ElevenLabs narration (BYOK)  
- [x] Play Asset Delivery module for optional voice packs  

## License

Apache-2.0 (intended). Confirm before publishing.
