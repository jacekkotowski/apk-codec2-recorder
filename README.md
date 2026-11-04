# Codec2 Recorder

Offline Android dictaphone that stores speech with Codec 2, from 3200 down to 450 bit/s. It also compresses existing audio files. Voice chain: rumble cut, denoiser with noise sample, gate, treble and consonant lift, leveler, compressor and look-ahead limiter.

Current version: 2.4. Roadmap in [PLAN.md](PLAN.md).

## Build

Needs: Android SDK with build-tools 36.0.0 and platforms;android-36, JDK 17 or newer, Python 3, zip, and NDK r28c in `$ANDROID_SDK_ROOT/android-ndk-r28c` or `ANDROID_NDK_ROOT`. Tested on Linux only.

1. Set `ANDROID_SDK_ROOT`.
2. Put `c2.keystore` and `signing.env` in the project folder. Both are in `apk-signing-keys.zip` and are not in git.
3. Run `bash build.sh`.

A different signing key means a new version can't install over the old one.

## Licences

- Codec 2: LGPL 2.1, `COPYING-codec2`, sources in `third_party/codec2`
- Codec2Recorder by tschudin, starting point: MIT, `LICENSE-Codec2Recorder`
