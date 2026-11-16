# Codec2 Recorder

Hand-built Android APK, no Gradle: aapt2, javac, d8, zipalign, apksigner; NDK clang for the native libraries with the 16 KB page flag. Package `pl.dyktafon`. WebView UI in `app/assets/index.html`, voice chain in `app/src/pl/dyktafon/`. Interface text is English. Roadmap in `PLAN.md`.

Two native libraries: `libvoice.so` from `third_party/opus-1.6.1` and `third_party/rade_c` (DRED coder and player for recordings, and RADE V1 voice messages; flags in `opusflags.sh` and `radeflags.sh`) and `libfdv.so` from `third_party/codec2-1.2` (FreeDV modems for the radio link, mode list in `fdvflags.sh`). Don't edit the upstream files: RADE needs a larger buffer in Opus's `dnn/nnet.c`, so `radeflags.sh` builds a patched copy of that one file and leaves the original alone. Codec 2 recording was removed in 2.7 on the owner's decision. Recording tests: `tests/t6/`, `tests/dred/`. RADE tests: `tests/dred/RA.java` (app layer against rade_c's own tools), `tests/rade/compare.py` (RADE V1 against 700D through simulated channels). RADE V2 is left out (stubs in `app/jni/rade_v2_stub.c`) until FreeDV releases it for use on air. Radio tests: `tests/radio/`, `tests/t5/`, `tests/ui_radio.js`. Self-test: `tests/selftest/run.sh` (`SelfTest.java` is pure Java; `SelfTestRun.java` is the Android side).

`.dred` files carry the DRED bitstream version (12 in Opus 1.6.1, still marked experimental). Keep this exact Opus version for as long as such files exist; a newer Opus goes in beside it, not instead of it.

DRED and RADE are not yet tested on a phone (2.7, 2.8). What still holds from the owner's phone in 2.4:

- Default audio source VOICE_RECOGNITION (6). MIC caused overload.
- The bar shows recording level: green, yellow above 0.85, red only on real mic overload.
- Voice activation and the VOICE_RECOGNITION audio source are different settings.
- RNNoise is ruled out: the owner judged it not good in an earlier session (said 2026-11-15). Don't propose it again; the app's own denoiser stays.

The voice chain keeps the 2.4 behaviour at 16 kHz: same filter corners and time constants, 40 ms frames, levels scaled with the limiter ceiling (`VoiceChain`). The ceiling for DRED is 18000, not 11000, because the DRED decoder overshoots peaks less than Codec 2 (measured on a computer, `tests/dred/chain.py`); playback raises the level twice through a look-ahead limiter. Don't change these without measuring, and write the confirmed DRED settings here after the phone test (`PLAN.md` stage 10).

## Where we are (2026-11-16)

- Current version 2.8.2 on `main` (2.8 plus the TEST tab), APK in `dist/`. Recordings are DRED (2.7); the RADE tab sends and receives RADE V1 voice messages (2.8). The self-test ran on the owner's phone on 2026-11-16 and passed (speed of DRED and RADE, modem loop through speaker and microphone). How DRED and RADE sound, recording and playback, two phones and a radio are still untested: the lists at the top of `PLAN.md`.
- "Radio link" means sound between the phone and a radio: FreeDV data modes DATAC1, DATAC3, DATAC4 for files (`libfdv.so`), and RADE V1 for voice (`libvoice.so`). Not HamDRM.
- `ROADMAP.md` (2026-11-15) is a review of both apps and a proposed big plan: shared core in one repository, a "Natural" sound profile, a task-based interface, radio use by hand, FlatRaster's picture. The owner asked to start it (2026-11-15). Decided that day: D2 to D10 follow the recommendations in its section 2 (D6, the app's name, still waits for a name). **D1 decided 2026-11-16: one repository** (this one, with `core/`, `apps/recorder/`, `apps/flatraster/`; both package names and signing keys stay). Stage 1 may move files, but its first step needs the stronger model (ROADMAP.md 9.1). Stage 0 (self-test and diagnostics) was built 2026-11-15 in both apps, each in its own repository (2.8.2, 0.15.2), and ran on the owner's phone. Stage 6b was started the same day: FlatRaster 0.16 (camera frames joined, smooth enlarging; not run on a phone; `PLAN.md` "Start here"). Stage 6a (picture coder measurement) was done 2026-11-16 on a computer: by the rule set beforehand the coder should not be built as the video coder; **D8 waits for the owner** (`PLAN.md` "Start here"; tools in `apk-codec2flatraster`, `tools/coder/`).
- Sister repositories: `apk-codec2flatraster` (FlatRaster, the same voice code since its 0.13 to 0.15), `apk-signing-keys` (private: keys for both apps).

## Starting a session

1. Fetch and list all branches before saying what exists: newer work may sit on an unmerged branch. `main` should hold everything.
2. Read `PLAN.md` "Start here".
   If the work is a stage of `ROADMAP.md`: its section 9 names the model for each stage (Sonnet, or the stronger model). Say at the start which model the session runs on (`get_session`) and whether it fits; a session on the stronger model may hand a Sonnet stage to a Sonnet helper and check the result. Keep model names out of commit messages.
3. To build: `bash setup.sh` once, then attach `apk-signing-keys` and copy `apk-codec2-recorder/c2.keystore` and `signing.env` into this folder, then `source ./env.sh && bash build.sh`.
4. Computer tests need `pip install numpy scipy pystoi` and, for the speech measurements, 16 kHz 16-bit mono `.raw` speech files. Last time they came from whisper.cpp's `samples/jfk.wav` (raw.githubusercontent.com) and the `wav/` folder of `drowe67/radae` (git clone works; GitHub's archive downloads were refused).
5. In a cloud session on Auto mode, the safety check may refuse to run programs built from downloaded code; the owner switches the mode to "Accept edits" and approves.

Rules:

- Never commit `*.keystore` or `signing.env`.
- Don't hand over a build unless it was built and tested from the current source. State plainly what was not tested on a phone.
- Raise `versionCode` and the version name with each delivered build.
- Put each delivered APK, signed with the real key, in `dist/` (replace the old one) and update the version and link in `README.md`. A test build that installs beside the release (`bash build.sh test`) sits in `dist/` beside it until the owner's go.
- Fresh machine: `bash setup.sh`, then `source ./env.sh && bash build.sh`. Keys are in the private `apk-signing-keys` repository, folder `apk-codec2-recorder`.
- Hand the APK to the owner as a file in the chat as well: the GitHub app does not download files from a repository.
- End every session with `PLAN.md` updated and the work pushed to `main` (the owner asked for `main` to be current).
