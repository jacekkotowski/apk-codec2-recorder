# Codec2 Recorder

Offline Android dictaphone and radio voice tool. Package `pl.dyktafon`, current version **2.6**.

- **Record** speech with Codec 2, from 3200 down to 450 bit/s (700C is about 6 kB per minute). Voice chain: rumble cut, denoiser with noise sample, gate, treble and consonant lift, leveler, compressor, look-ahead limiter. It also compresses existing audio files.
- **Send files through an SSB radio by sound alone.** The phone's speaker plays into the radio's microphone; on the other side a phone's microphone listens to the radio's speaker. Recordings, text, small pictures, anything up to 100 kB. FreeDV data modes with LDPC error correction, plus repair frames so a one-way broadcast survives lost frames.
- **Live voice** through the same path with FreeDV 700D or 700E (push to talk), compatible at the modem level with the FreeDV program.

No internet permission, no accounts.

## Status

Recording settings were confirmed on the owner's phone in 2.4. **The radio link (2.5, 2.6) is tested on a computer only**, through simulated channels: not yet on a phone, not on a radio. Measurements, roadmap and the first phone test are in [PLAN.md](PLAN.md).

## Download

[`dist/Codec2Recorder-2.6.apk`](dist/Codec2Recorder-2.6.apk), signed with the project key, installs over earlier versions. Android will ask to allow installing from this source.

## Using the radio link

| Tab | What it does |
|---|---|
| Record | REC, compression, Denoise, Voice activation. Microphone, source, level and audio options are behind SETTINGS |
| Files | SEND on each recording (shows time on air), SEND ANY FILE, RECEIVE, TEST TONE. Speed: Fast 980 bit/s, Robust 320 bit/s, Very robust 87 bit/s. Repair: +25% to +200%, or repeat until stopped |
| Live | START, then tap TALK to transmit and tap again to listen |

Both sides need the same speed. Hold the phone close to the radio's microphone: in simulation, room echo as loud as the direct sound stopped the Fast and Robust speeds. A received recording joins the list; any other file goes to `Download/Codec2Recorder`.

## Start or continue work

Everything needed is in this repository except the signing key.

1. Clone, then run `bash setup.sh` once. It installs the Android SDK parts (build-tools 36.0.0, platform 36, NDK r28c, about 3 GB) and writes `env.sh`. Needs Linux, JDK 17 or newer, curl, unzip, zip.
2. Copy `c2.keystore` and `signing.env` from the private repository `apk-signing-keys`, folder `apk-codec2-recorder`, into the project folder. They are never committed here. Without them a build can't install over the released app.
3. `source ./env.sh && bash build.sh` gives `Codec2Recorder.apk`.
4. Read `CLAUDE.md` (rules and confirmed settings) and `PLAN.md` (roadmap, what is tested, what is not).

With Claude Code: open the repository, cloud or desktop, and say "Read CLAUDE.md and PLAN.md and continue with the next roadmap stage". In a cloud session also ask it to attach `apk-signing-keys`. Desktop with the phone on USB is the better place for phone testing.

For each delivered build: raise `versionCode` and the version name in `app/AndroidManifest.xml`, run the tests, and replace the APK in `dist/`.

## Tests (on a computer)

- `javac -d tests/t5 app/src/pl/dyktafon/Fountain.java tests/t5/FT.java && java -cp tests/t5 pl.dyktafon.FT`: file transport with repair frames.
- `bash tests/radio/build.sh`, then `python3 tests/radio/run.py SPEECH.raw`: files and live voice through the modem and simulated channels (noise, fading, speaker-to-microphone echo, distortion, clock difference). Needs gcc, a JDK and numpy; SPEECH.raw is about 10 s of 8 kHz 16-bit speech.
- `node tests/ui_radio.js`: the three-tab page against a stub of the app. Needs Playwright.

## Layout

- `app/assets/index.html`: the whole interface (WebView).
- `app/src/pl/dyktafon/`: `MainActivity` (bridge to the page, playback, file compression), `RecService` (recording), `VoiceChain` and its parts, `C2` (codec JNI). Radio link: `Fountain` (repair frames), `RadioLink` (file to modem audio and back), `Radio` (modem JNI), `RadioService` (Android audio).
- `app/jni/c2jni.c` with `third_party/codec2` (Codec 2 1.0) builds `libc2.so` for recording. `app/jni/fdvjni.c` with `third_party/codec2-1.2` builds `libfdv.so`, the modem; its mode list is in `fdvflags.sh`. Two copies because 1.2 dropped the 450 bit/s mode.
- `build.sh`: the whole build, no Gradle. `setup.sh`: SDK install. `dist/`: the current APK.

## Licences

- Codec 2: LGPL 2.1, `COPYING-codec2`, sources in `third_party/codec2` (1.0) and `third_party/codec2-1.2` (1.2.0, unchanged upstream files)
- Codec2Recorder by tschudin, starting point: MIT, `LICENSE-Codec2Recorder`
