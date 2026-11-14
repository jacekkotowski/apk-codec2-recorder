# Codec2 Recorder

Offline Android dictaphone and radio voice tool. Package `pl.dyktafon`, current release **2.6**. Version **2.7** is out as a test build that installs beside 2.6.

- **Record** speech at 2 to 10 kB per minute. From 2.7 the coder is DRED, the neural speech coder inside the Opus library; up to 2.6 it was Codec 2. Voice chain: rumble cut, denoiser with noise sample, gate, treble and consonant lift, leveler, compressor, look-ahead limiter. It also compresses existing audio files.
- **Send files through an SSB radio by sound alone.** The phone's speaker plays into the radio's microphone; on the other side a phone's microphone listens to the radio's speaker. Recordings, text, small pictures, anything up to 100 kB. FreeDV data modes with LDPC error correction, plus repair frames so a one-way broadcast survives lost frames.
- **Live voice** with FreeDV 700D (push to talk) in 2.6. Hidden in 2.7; it returns in 2.8 carrying DRED.

## DRED recordings (2.7)

DRED sounds natural at sizes where Codec 2 sounded robotic, but the voice is rebuilt by a neural network: it may not sound quite like the speaker, and in heavy noise it can produce clear-sounding wrong words. Four levels:

| Level | About, per minute | Note |
|---|---|---|
| DRED best | 10 kB | Clearest |
| DRED balanced | 6 kB | Same size as Codec 2 700C, clearer |
| DRED small | 4 kB | Default |
| DRED smallest | 2 kB | May lose words |

- **Only this app plays `.dred` files.** An ordinary player ignores them. To share a recording with someone else, use SAVE WAV.
- **`.c2` files from 2.6 no longer play in 2.7.** They stay in the list with SHARE and DELETE. To keep one, open it in 2.6 and save it as WAV before upgrading.
- The DRED format is still marked experimental in Opus 1.6.1. Each file names its bitstream version (12), and the app keeps this exact library for as long as such files exist.

No internet permission, no accounts.

## Status

Recording settings were confirmed on the owner's phone in 2.4, for Codec 2. **DRED recording (2.7) is tested on a computer only**: nothing that touches Android audio has run on a phone. **The radio link (2.5, 2.6) is also tested on a computer only**, through simulated channels: not yet on a phone, not on a radio. Measurements, roadmap and the phone test lists are in [PLAN.md](PLAN.md).

## Download

- [`dist/Codec2Recorder-2.6.apk`](dist/Codec2Recorder-2.6.apk): the current release, Codec 2. Signed with the project key, installs over earlier versions.
- [`dist/Codec2Recorder-2.7-test1.apk`](dist/Codec2Recorder-2.7-test1.apk): test build of 2.7 with DRED. It is a separate app, "Recorder test" (package `pl.dyktafon.test`), so it installs beside 2.6 without touching its recordings. Uninstalling it deletes only its own recordings.

Android will ask to allow installing from this source.

## Using the radio link

| Tab | What it does |
|---|---|
| Record | REC, compression level, Denoise, Voice activation. Microphone, source, level and audio options are behind SETTINGS |
| Files | SEND on each recording (shows time on air), SEND ANY FILE, RECEIVE, TEST TONE. Speed: Fast 980 bit/s, Robust 320 bit/s, Very robust 87 bit/s. Repair: +25% to +200%, or repeat until stopped |
| Live | START, then tap TALK to transmit and tap again to listen. In 2.6 only; hidden in 2.7 |

Both sides need the same speed. Hold the phone close to the radio's microphone: in simulation, room echo as loud as the direct sound stopped the Fast and Robust speeds. A received recording joins the list; any other file goes to `Download/Codec2Recorder`.

## Start or continue work

Everything needed is in this repository except the signing key.

1. Clone, then run `bash setup.sh` once. It installs the Android SDK parts (build-tools 36.0.0, platform 36, NDK r28c, about 3 GB) and writes `env.sh`. Needs Linux, JDK 17 or newer, curl, unzip, zip.
2. Copy `c2.keystore` and `signing.env` from the private repository `apk-signing-keys`, folder `apk-codec2-recorder`, into the project folder. They are never committed here. Without them a build can't install over the released app.
3. `source ./env.sh && bash build.sh` gives `Codec2Recorder.apk`. `bash build.sh test` gives `Codec2Recorder-test.apk`, the build that installs beside the real app.
4. Read `CLAUDE.md` (rules and confirmed settings) and `PLAN.md` (roadmap, what is tested, what is not).

With Claude Code: open the repository, cloud or desktop, and say "Read CLAUDE.md and PLAN.md and continue with the next roadmap stage". In a cloud session also ask it to attach `apk-signing-keys`. Desktop with the phone on USB is the better place for phone testing.

For each delivered build: raise `versionCode` and the version name in `app/AndroidManifest.xml`, run the tests, and replace the APK in `dist/`.

## Tests (on a computer)

- `javac -d tests/t5 app/src/pl/dyktafon/Fountain.java tests/t5/FT.java && java -cp tests/t5 pl.dyktafon.FT`: file transport with repair frames.
- `javac -d tests/t6/out app/src/pl/dyktafon/Dred.java tests/t6/DT.java && java -cp tests/t6/out pl.dyktafon.DT`: the `.dred` file format.
- `bash tests/dred/build.sh` builds the DRED library for this computer with the app's flags, then:
  - `java -cp tests/dred/out pl.dyktafon.RS`: the resampler used for compressing existing files.
  - `java -Djava.library.path=tests/dred/out -cp tests/dred/out pl.dyktafon.JT Q in.raw out.dred out.raw`: the app's JNI coder and player; compare with `tests/dred/dredtest enc2`/`dec2`.
  - `java -Djava.library.path=tests/dred/out -cp tests/dred/out pl.dyktafon.RP ...`: the whole recording path without Android.
  - `python3 tests/dred/run.py SPEECH_DIR OUT_DIR` (DRED levels, size and intelligibility) and `python3 tests/dred/chain.py SPEECH_DIR OUT_DIR` (the recording path on clean and noisy speech). Need numpy, scipy, pystoi and 16 kHz 16-bit speech files.
- `bash tests/radio/build.sh`, then `python3 tests/radio/run.py SPEECH.raw`: files and live voice (FreeDV 700D) through the modem and simulated channels (noise, fading, speaker-to-microphone echo, distortion, clock difference). Needs gcc, a JDK and numpy; SPEECH.raw is about 10 s of 8 kHz 16-bit speech.
- `node tests/ui_radio.js`: the page against a stub of the app. Needs Playwright.

## Layout

- `app/assets/index.html`: the whole interface (WebView).
- `app/src/pl/dyktafon/`: `MainActivity` (bridge to the page, playback, file compression), `RecService` (recording), `Recorder` (voice chain to `.dred` file), `VoiceChain` and its parts, `Resampler`, `Dred` (file format), `DredLib` (coder and player JNI). Radio link: `Fountain` (repair frames), `RadioLink` (file to modem audio and back), `Radio` (modem JNI), `RadioService` (Android audio).
- `app/jni/dredjni.c` with `third_party/opus-1.6.1` builds `libdred.so`, the DRED coder and player; flags and source list in `opusflags.sh`. `app/jni/fdvjni.c` with `third_party/codec2-1.2` builds `libfdv.so`, the modem; its mode list is in `fdvflags.sh`.
- `build.sh`: the whole build, no Gradle. `setup.sh`: SDK install. `dist/`: the current APK.

## Licences

- Opus 1.6.1 with DRED: BSD, three clauses, `COPYING-opus`, sources in `third_party/opus-1.6.1` (unchanged upstream files; the folder holds only what the build needs)
- Codec 2 1.2 (the FreeDV modems): LGPL 2.1, `COPYING-codec2`, sources in `third_party/codec2-1.2` (unchanged upstream files)
- Codec2Recorder by tschudin, starting point: MIT, `LICENSE-Codec2Recorder`
