# Codec2 Recorder

Hand-built Android APK, no Gradle: aapt2, javac, d8, zipalign, apksigner; NDK clang for the native libraries with the 16 KB page flag. Package `pl.dyktafon`. WebView UI in `app/assets/index.html`, voice chain in `app/src/pl/dyktafon/`. Interface text is English. Roadmap in `PLAN.md`.

Two native libraries: `libvoice.so` from `third_party/opus-1.6.1` and `third_party/rade_c` (DRED coder and player for recordings, and RADE V1 voice messages; flags in `opusflags.sh` and `radeflags.sh`) and `libfdv.so` from `third_party/codec2-1.2` (FreeDV modems for the radio link, mode list in `fdvflags.sh`). Don't edit the upstream files: RADE needs a larger buffer in Opus's `dnn/nnet.c`, so `radeflags.sh` builds a patched copy of that one file and leaves the original alone. Codec 2 recording was removed in 2.7 on the owner's decision. Recording tests: `tests/t6/`, `tests/dred/`. RADE tests: `tests/dred/RA.java` (app layer against rade_c's own tools), `tests/rade/compare.py` (RADE V1 against 700D through simulated channels). RADE V2 is left out (stubs in `app/jni/rade_v2_stub.c`) until FreeDV releases it for use on air. Radio tests: `tests/radio/`, `tests/t5/`, `tests/ui_radio.js`.

`.dred` files carry the DRED bitstream version (12 in Opus 1.6.1, still marked experimental). Keep this exact Opus version for as long as such files exist; a newer Opus goes in beside it, not instead of it.

DRED settings are not yet confirmed on a phone (2.7 test build). What still holds from the owner's phone in 2.4:

- Default audio source VOICE_RECOGNITION (6). MIC caused overload.
- The bar shows recording level: green, yellow above 0.85, red only on real mic overload.
- Voice activation and the VOICE_RECOGNITION audio source are different settings.

The voice chain keeps the 2.4 behaviour at 16 kHz: same filter corners and time constants, 40 ms frames, levels scaled with the limiter ceiling (`VoiceChain`). The ceiling for DRED is 18000, not 11000, because the DRED decoder overshoots peaks less than Codec 2 (measured on a computer, `tests/dred/chain.py`); playback raises the level twice through a look-ahead limiter. Don't change these without measuring, and write the confirmed DRED settings here after the phone test (`PLAN.md` stage 10).

Rules:

- Never commit `*.keystore` or `signing.env`.
- Don't hand over a build unless it was built and tested from the current source. State plainly what was not tested on a phone.
- Raise `versionCode` and the version name with each delivered build.
- Put each delivered APK, signed with the real key, in `dist/` (replace the old one) and update the version and link in `README.md`. A test build that installs beside the release (`bash build.sh test`) sits in `dist/` beside it until the owner's go.
- Fresh machine: `bash setup.sh`, then `source ./env.sh && bash build.sh`. Keys are in the private `apk-signing-keys` repository, folder `apk-codec2-recorder`.
