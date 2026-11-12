# Codec2 Recorder

Hand-built Android APK, no Gradle: aapt2, javac, d8, zipalign, apksigner; NDK clang for Codec 2 with the 16 KB page flag. Package `pl.dyktafon`. WebView UI in `app/assets/index.html`, voice chain in `app/src/pl/dyktafon/`. Interface text is English. Roadmap in `PLAN.md`.

Two native libraries: `libc2.so` from `third_party/codec2` (1.0, recording, keeps the 450 mode) and `libfdv.so` from `third_party/codec2-1.2` (FreeDV modems for the radio link, mode list in `fdvflags.sh`). Don't edit the upstream files. Radio tests: `tests/radio/`, `tests/t5/`, `tests/ui_radio.js`.

Settings confirmed good on the owner's phone in 2.4. Don't change them without measuring:

- Default audio source VOICE_RECOGNITION (6). MIC caused overload.
- `CEILING = 11000`, `AUTO_PEAK = 8000`, `MAX_AUTO_GAIN = 40`. The Codec 2 decoder overshoots peaks about 2 to 2.4 times, so the encoder level leaves room for that.
- The bar shows recording level: green, yellow above 0.85, red only on real mic overload.
- Voice activation and the VOICE_RECOGNITION audio source are different settings.

Rules:

- Never commit `*.keystore` or `signing.env`.
- Don't hand over a build unless it was built and tested from the current source. State plainly what was not tested on a phone.
- Raise `versionCode` and the version name with each delivered build.
