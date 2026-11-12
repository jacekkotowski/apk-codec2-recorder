# Codec2 Recorder

Offline Android dictaphone that stores speech with Codec 2, from 3200 down to 450 bit/s. It also compresses existing audio files. Voice chain: rumble cut, denoiser with noise sample, gate, treble and consonant lift, leveler, compressor and look-ahead limiter.

It can also pass speech through an SSB radio by sound alone: the phone's speaker plays into the radio's microphone and the phone's microphone listens to the radio's speaker. Recordings and any other small file (text, pictures) go as files with error correction (FreeDV data modes plus repair frames), and live voice goes as FreeDV 700D or 700E.

Current version: 2.6. The radio link (2.5, 2.6) is tested on a computer only, not on a phone or a radio. Roadmap and measurements in [PLAN.md](PLAN.md).

## Build

Needs: Android SDK with build-tools 36.0.0 and platforms;android-36, JDK 17 or newer, Python 3, zip, and NDK r28c in `$ANDROID_SDK_ROOT/android-ndk-r28c` or `ANDROID_NDK_ROOT`. Tested on Linux only.

1. Set `ANDROID_SDK_ROOT`.
2. Put `c2.keystore` and `signing.env` in the project folder. Both are in `apk-signing-keys.zip` and are not in git.
3. Run `bash build.sh`.

A different signing key means a new version can't install over the old one.

## Tests (on a computer)

- `javac -d tests/t5 app/src/pl/dyktafon/Fountain.java tests/t5/FT.java && java -cp tests/t5 pl.dyktafon.FT`: file transport with repair frames.
- `bash tests/radio/build.sh`, then `python3 tests/radio/run.py SPEECH.raw`: files and live voice through the modem and simulated channels (noise, fading, speaker-to-microphone echo, distortion, clock difference). Needs gcc, a JDK and numpy.
- `node tests/ui_radio.js`: the three-tab page against a stub of the app. Needs Playwright.

## Layout

- `app/src/pl/dyktafon/`: recorder and voice chain; radio link in `Fountain.java` (repair frames), `RadioLink.java` (file to modem audio and back), `Radio.java` (modem JNI), `RadioService.java` (Android audio).
- `app/jni/c2jni.c` with `third_party/codec2` (Codec 2 1.0) builds `libc2.so` for recording. `app/jni/fdvjni.c` with `third_party/codec2-1.2` builds `libfdv.so`, the modem. Two copies because 1.2 dropped the 450 bit/s mode.

## Licences

- Codec 2: LGPL 2.1, `COPYING-codec2`, sources in `third_party/codec2` (1.0) and `third_party/codec2-1.2` (1.2.0, unchanged upstream files)
- Codec2Recorder by tschudin, starting point: MIT, `LICENSE-Codec2Recorder`
