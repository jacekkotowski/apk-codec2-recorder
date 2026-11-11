# Codec2 Recorder

Hand-built Android APK, no Gradle: aapt2, javac, d8, zipalign, apksigner; NDK clang for Codec 2 with the 16 KB page flag. Package `pl.dyktafon`. WebView UI in `app/assets/index.html`, voice chain in `app/src/pl/dyktafon/`. Interface text is English. Roadmap in `PLAN.md`.

Settings confirmed good on the owner's phone in 2.4. Don't change them without measuring:

- Default audio source VOICE_RECOGNITION (6). MIC caused overload.
- `CEILING = 11000`, `AUTO_PEAK = 8000`, `MAX_AUTO_GAIN = 40`. The Codec 2 decoder overshoots peaks about 2 to 2.4 times, so the encoder level leaves room for that.
- The bar shows recording level: green, yellow above 0.85, red only on real mic overload.
- Voice activation and the VOICE_RECOGNITION audio source are different settings.

Rules:

- Never commit `*.keystore` or `signing.env`.
- Don't hand over a build unless it was built and tested from the current source. State plainly what was not tested on a phone.
- Raise `versionCode` and the version name with each delivered build.

## Heart Sounds (`heart/`)

Second app, package `pl.heartsounds`, same hand build without the NDK: `bash heart/build.sh`, signing from the root `signing.env`. Records raw 48 kHz WAV from the phone mic on the chest; band-pass, noise reduction and graph are applied for display, playback and export only. Status in `heart/PLAN.md`.

- It copies the layout and Limiter from Codec2 Recorder but shares no files. Changes in `heart/` must not touch `app/`.
- The voice-app settings above do not apply to it: it uses UNPROCESSED, no auto level, no gate.
- Run `bash heart/tests/run.sh` and `node heart/tests/ui.js` before handing over a build.
- Never show anything as an ECG, a diagnosis, a named disease, or a heart rate that was not measured. The murmur-like-sound flag is an acoustic flag with its caveat; its limit (`Screen.SYS_DB`) was chosen on CirCor data, so re-run the evaluation in `heart/docs/validation.md` before changing it or `Analysis.java`.

