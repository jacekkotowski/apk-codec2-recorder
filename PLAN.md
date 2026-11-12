# Codec2 Recorder plan

Last updated: 2026-11-12

## Roadmap

Each stage ends with something that can be checked. Stages 1 to 3 need the owner's phone and radio; the rest can be prepared on a computer.

| # | Stage | Done when | State |
|---|---|---|---|
| 0 | Radio link built and measured on a computer (2.5, 2.6) | Files and live voice pass simulated channels | Done 2026-11-12 |
| 1 | First phone test of 2.6 | 2.6 installs over 2.4, recording behaves as in 2.4, and a short recording crosses a table from phone to phone in Robust | Waiting for the owner |
| 2 | Fix what the phone shows | A file passes phone to phone in all three speeds; live voice is understandable both ways | Not started |
| 3 | Through a real radio | A file and live voice pass radio to radio; live voice is heard by the FreeDV program on a PC | Not started |
| 4 | Cable instead of air | Send and receive through a headset cable or USB audio, with transmitter keying | Not started |
| 5 | Two-way link | Receiver reports missing frames, sender resends only those; short text messages | Not started |
| 6 | FlatRaster over radio | The same link in apk-codec2flatraster, with a low-bitrate Radio preset | Not started |
| 7 | Side tracks, any time | Offline transcription; RNNoise comparison; background music removal | Plans below |

### Stage 1: first phone test (owner)

1. Install `dist/Codec2Recorder-2.6.apk` over 2.4. Make one recording with the usual settings and play it: nothing should differ from 2.4.
2. Files tab, TEST TONE on phone A at about three quarters media volume. It should be loud and clean, not rattling.
3. Phone B: Files, RECEIVE, speed Robust. Phone A: same speed, SEND on a short recording. Hold A's speaker about 5 cm from B's microphone.
4. Note what B shows: "Signal locked" or not, SNR, "Got N of M", mic %. Then repeat at 30 cm.
5. SEND ANY FILE with a small text file; look for it in Download/Codec2Recorder on B.
6. Live tab on both, 700D: START on both, TALK on A. Try A with a headset microphone, then without and with "Cancel this phone's own sound".

Report the numbers from step 4 and what was heard in step 6.

### Stage 2: likely fixes, depending on stage 1

- No lock at all: check microphone source (Unprocessed against Voice recognition), add a switch for it; save what the microphone heard to a WAV for a look on a computer.
- Lock but frames lost: speaker distortion (lower the modem level in the app, not only the volume), or echo (closer coupling).
- Mic % near 100: add an input level warning.
- Sending stops with the screen off: add a wake lock.
- Live talk garbled by the phone's own speaker: make the headset route or echo cancelling the default, or add a short "record, then send" mode in the Live tab.

### Stage 3: through a radio

- Transmitter into a dummy load first. Set the level with TEST TONE so ALC barely moves; speech processor off.
- VOX: check the 1 s lead-in tone opens it and the short gaps between bursts don't drop it; make both adjustable if needed.
- Station identification: most licence rules need the call sign in plain form. Add a call sign setting and send it before each data transmission (voice announcement by the operator at first, CW ID later).
- State the bandwidth of the chosen mode on screen (Fast 1700 Hz, Robust 500 Hz, Very robust 250 Hz, 700D about 1.1 kHz, 700E about 1.5 kHz).
- Compare live voice with the FreeDV program on a PC, both directions.

### Stage 4: cable

- Output and input through a wired or USB headset adapter; choose devices explicitly so the modem never goes to the wrong speaker.
- Keying: VOX first, then a USB serial RTS/DTR line.

### Stage 5: two-way link

- Short acknowledgement frames in DATAC13 or DATAC14 from upstream; the receiver lists missing frames, the sender sends only repair frames for them.
- Text messages typed in the app, sent as small files.

### Housekeeping

- `libfdv.so` is about 1.3 MB per processor type, mostly error-correction tables for modes the app doesn't use. Trim without editing upstream files if possible.
- `tests/ui.py` needs Python Playwright, which the cloud sessions don't have; port it to Node like `tests/ui_radio.js`.

## Status

| Part | State |
|---|---|
| Record, pause, play, seek, rename, delete | Built, not yet confirmed on Android |
| All Codec 2 modes from 3200 down to 450 | Encode and decode tested on a computer |
| Recording with the screen off | Built, not yet confirmed on Android |
| Share, save .c2, save WAV, open .c2 | Built, not yet confirmed on Android |
| Denoise, noise sample, continuous adaptation | Tested on synthetic speech with added noise |
| Voice activation with 400 ms pre-roll | Tested on synthetic speech with added noise |
| Microphone choice, rec level, audio source, treble boost, voice sensitivity and hold, denoise strength | Built, waiting for phone test |
| RNNoise comparison | Blocked: running downloaded code needs the user's approval. Watch for cut word endings |
| Compress existing audio files through the voice path | Built, waiting for phone test |
| Rumble cut and consonant lift | Measured on synthetic speech, waiting for phone test |
| Look-ahead limiter and noise gate | Measured on synthetic speech, waiting for phone test |
| Bluetooth microphones | Built, not tested: no Bluetooth headset available in testing |
| Remove background music | Not started, plan below |
| Radio: sending and receiving recordings as files, with repair frames (2.5) | Modem and transport tested on a computer through simulated channels. Android audio part built, not run anywhere |
| Radio: live voice, FreeDV 700D and 700E (2.5) | Modem tested on a computer. Android part built, not run anywhere |
| Record, Live and Files tabs (2.5) | Page tested against a stub of the app, not on a phone |
| Offline transcription, Polish and English | Not started, plan below |

## Radio link: what was built and measured

Decided 2026-11-12: use the FreeDV modems from upstream Codec 2 (LGPL 2.1, as now) instead of HamDRM (GPL-3.0, QAM that needs a clean signal, no Codec 2 voice). Build it here first; FlatRaster copies it later.

Modes, from upstream `README_data.md` (commit 310777b, 2026-03-17). SNR is the project's own figure for a poor fading channel, not measured here.

| Mode | Use | Bandwidth | Net speed | Bytes per frame | Frame | Works at |
|---|---|---|---|---|---|---|
| FreeDV 700D / 700E | Live voice, carries Codec 2 700C | 1.1 to 1.5 kHz | live | | | not yet looked up |
| DATAC1 | Files, good signal | 1700 Hz | 980 bit/s | 510 | 4.18 s | 5 dB |
| DATAC3 | Files, weak signal | 500 Hz | 321 bit/s | 126 | 3.19 s | 0 dB |
| DATAC4 | Files, very weak signal | 250 Hz | 87 bit/s | 54 | 5.17 s | -4 dB |

Each modem frame has LDPC error correction and a CRC16, so a frame arrives whole or not at all.

### Built in 2.5 and 2.6

- `third_party/codec2-1.2`: unchanged upstream library sources, built into `libfdv.so` with only 700D, 700E, DATAC1, DATAC3, DATAC4. The recorder's `libc2.so` (Codec 2 1.0) is untouched, because 1.2 dropped the 450 bit/s mode.
- `Fountain.java`: cuts a file into modem frames and adds any number of repair frames. The receiver rebuilds the file from any k different frames (k = number of plain frames), in any order. Carries name, length and CRC32.
- `RadioLink.java`: 1 s lead-in tone (opens the transmitter's VOX), then bursts of 4 frames, each burst with its own preamble and postamble so the receiver can lock on again after a gap. "Repeat until stopped" keeps making new repair frames.
- `RadioService.java`: send, receive, live voice (tap to talk), test tone. Receives with the Unprocessed microphone source where the phone has it, else Voice recognition. Plays as plain media sound.
- Page: Record (settings folded behind a button, values unchanged), Live, Files.
- 2.6: SEND ANY FILE (text, pictures, anything up to 100 kB) through the same transport. A received file that is not a recording is saved to Download/Codec2Recorder. Text and binary files with non-ASCII names went through the computer test unchanged; the Android file picker and the save to Downloads are not run anywhere.

### Measured on a computer, 2026-11-12 (`tests/radio/run.py`)

Simulated channels, not real ones. SNR is in a 3 kHz band. "Speaker to mic" is a small-speaker response plus room echo with 0.3 s decay.

#### Files: 2.6 kB with 50% repair frames, received complete in N of 3 runs

| Channel | Fast | Robust | Very robust |
|---|---|---|---|
| Clean | 3/3 | 3/3 | 3/3 |
| Noise, SNR 10 dB | 3/3 | 3/3 | 3/3 |
| Noise, SNR 5 dB | 3/3 | 3/3 | 3/3 |
| Noise, SNR 2 dB | 3/3 | 3/3 | 3/3 |
| Noise, SNR 0 dB | 0/3 | 3/3 | 3/3 |
| Noise, SNR -3 dB | 0/3 | 3/3 | 3/3 |
| Noise, SNR -5 dB | 0/3 | 0/3 | 3/3 |
| HF fading, SNR 10 dB | 3/3 | 3/3 | 3/3 |
| HF fading, SNR 5 dB | 3/3 | 3/3 | 3/3 |
| HF fading, SNR 0 dB | 0/3 | 2/3 | 1/3 |
| Speaker to mic, close, distorted, clocks 100 ppm apart, SNR 15 dB | 3/3 | 3/3 | 3/3 |
| Speaker to mic, echo 5 dB below direct, SNR 10 dB | 3/3 | 3/3 | 3/3 |
| Speaker to mic, echo as loud as direct | 0/3 | 0/3 | 2/3 |
| 8 s gap in the middle | 3/3 | 3/3 | 3/3 |
| Time on air, seconds | 52.5 | 119.8 | 444.5 |

Frames decoded from 10 minutes of pure noise: Fast 0, Robust 0, Very robust 0

#### Live voice: time in sync, and how well the decoded loudness contour matches the clean-channel decode (1.00 = same)

| Channel | 700D | 700E |
|---|---|---|
| Clean | 98%, 1.00 | 99%, 1.00 |
| Noise, SNR 5 dB | 98%, 0.99 | 99%, 0.99 |
| Noise, SNR 2 dB | 98%, 0.99 | 99%, 0.99 |
| Noise, SNR 0 dB | 98%, 0.98 | 99%, 0.73 |
| Noise, SNR -2 dB | 98%, 0.88 | 99%, 0.53 |
| HF fading, SNR 5 dB | 98%, 0.79 | 99%, 0.93 |
| Speaker to mic, close, distorted, clocks 100 ppm apart, SNR 15 dB | 98%, 0.99 | 99%, 1.00 |
| Speaker to mic, echo 5 dB below direct, SNR 10 dB | 98%, 0.95 | 99%, 0.98 |
| Speaker to mic, close, with the decoded voice leaking into the mic 6 dB below the signal | 98%, 0.99 | 99%, 0.94 |

What this says:

- The phone must be held close to the radio's microphone. When echo is as loud as the direct sound, files fail in Fast and Robust.
- Distortion, a 100 ppm clock difference and an 8 s gap did not stop a file.
- 700D holds up better in plain noise, 700E in fading.
- The loudness-contour figure shows the voice came through at the right moments. It is not a measure of how clear it sounds.

### Not tested, needs the phone

- Everything in `RadioService`: whether the phone's speaker and microphone pass the modem tones, what the Unprocessed source really does, levels.
- Live talking into the phone itself: the speaker's modem sound reaches the phone's own microphone. Two answers are built, both untried: a headset microphone, or the "Cancel this phone's own sound" switch (Android's echo canceller).
- Live listening without earphones: decoded voice re-enters the microphone. In simulation at 6 dB below the signal it did no harm.
- Any real radio, VOX timing, ALC.

## Side track: offline transcription

The user downloads a language model once, inside the app. After that, transcription needs no internet.

### Engine options

Sizes are from the projects' own model pages, checked on 2026-11-03.

| Engine | Model | Download | Languages | Notes |
|---|---|---|---|---|
| Vosk | vosk-model-small-pl-0.22 | 50 MB | Polish | Apache 2.0, one language per model |
| Vosk | vosk-model-small-en-us-0.15 | 40 MB | English | Apache 2.0, described as lightweight for Android |
| whisper.cpp | tiny | 75 MiB | Both, one file | Often more accurate, slower on a phone |
| whisper.cpp | base | 142 MiB | Both, one file | More accurate than tiny, slower still |
| whisper.cpp | small | 466 MiB | Both, one file | Probably too heavy for routine phone use |

### Proposed approach

Start with whisper.cpp base, with tiny as the faster option. One file covers Polish and English, and it needs no live connection to the recording process. Run it after recording, not during it.

Measure Vosk against it before final selection, because Vosk may suit long recordings or older phones better. These accuracy and speed expectations are not yet measured.

### Important design choice

Codec 2 removes detail that speech recognition uses. At 700C and 450, transcribing the decoded file is likely to be noticeably worse than transcribing the original sound.

The app should offer two paths:

1. **Best quality:** while recording, also keep the denoised 8 kHz or 16 kHz audio in a temporary file, transcribe it, then delete it.
2. **Received or imported .c2 files:** decode, then transcribe, and label the result as lower confidence.

### Build steps

1. Add a Models screen with size, download progress, checksum check and delete.
2. Build whisper.cpp for arm64 with the NDK, with 16 KB page support.
3. Add a background transcription job with progress, in a foreground service.
4. Save text next to the recording as `name.txt`, with timestamps, and add copy and share.
5. Add a language choice: Automatic, Polish, English.
6. Measure on a real phone: speed relative to audio length, battery use and accuracy on Polish and English recordings at each Codec 2 mode.

### Acceptance tests

- Works in flight mode after the model is downloaded.
- Polish diacritics are correct in saved text.
- A failed or interrupted download can resume or restart cleanly.
- Recording still works while no model is installed.

### Risks

- Phones with little memory may struggle with base.
- The download needs internet once, so the app would gain the internet permission. It should be used only when the user starts a model download.
- Accuracy on noisy HF audio will be well below clean-speech accuracy.

## Later: background music removal

Noise reduction assumes steady noise. Music changes all the time and overlaps speech in frequency, so this filter can't separate it reliably.

Separating speech from music needs a neural source-separation model, such as Demucs. Run it offline on recordings and files, not live, and download it once like the language models. Size and phone speed still need measuring. A test also needs the user's approval to run downloaded code here.

## Files to keep

`c2.keystore` signs the app. Without it, later versions can't install over the current one.

## Licenses

- Codec 2: LGPL 2.1
- Codec2Recorder (starting point for features and the library setup): MIT
- Vosk and its small models listed here: Apache 2.0
