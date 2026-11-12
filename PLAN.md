# Codec2 Recorder plan

Last updated: 2026-11-12

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
| Radio: file transport with repair frames (`Fountain.java`) | Tested on a computer (`tests/t5`) |
| Radio: FreeDV modem, send and receive screens, live voice | Not started: needs approval to compile upstream Codec 2 here |
| Offline transcription, Polish and English | Not started, plan below |

## Next step 1: radio link with FreeDV modems

Decided 2026-11-12: use the FreeDV modems from upstream Codec 2 (LGPL 2.1, as now) instead of HamDRM (GPL-3.0, QAM that needs a clean signal, no Codec 2 voice). Build it here first; FlatRaster copies it later.

Modes, from upstream `README_data.md` (commit 310777b, 2026-03-17). SNR is the project's own figure for a poor fading channel, not measured here.

| Mode | Use | Bandwidth | Net speed | Bytes per frame | Frame | Works at |
|---|---|---|---|---|---|---|
| FreeDV 700D / 700E | Live voice, carries Codec 2 700C | 1.1 to 1.5 kHz | live | | | not yet looked up |
| DATAC1 | Files, good signal | 1700 Hz | 980 bit/s | 510 | 4.18 s | 5 dB |
| DATAC3 | Files, weak signal | 500 Hz | 321 bit/s | 126 | 3.19 s | 0 dB |
| DATAC4 | Files, very weak signal | 250 Hz | 87 bit/s | 54 | 5.17 s | -4 dB |

Each modem frame has LDPC error correction and a CRC16, so a frame arrives whole or not at all.

### Done

- `Fountain.java`: cuts a file into modem frames and adds any number of repair frames. The receiver rebuilds the file from any k different frames (k = number of plain frames), in any order, so a one-way broadcast survives lost frames without asking for resends. Carries name, length and CRC32. Test: `tests/t5/FT.java`, frame sizes 54, 126, 510, loss up to 50%; one extra frame was needed in 9 of 2000 runs.

### To do

1. Add the upstream modem sources to `third_party/codec2`, only 700D, 700E, DATAC1, DATAC3, DATAC4 enabled. The current copy is version 1.0 with voice files only. Check that existing `.c2` files decode to identical samples.
2. Computer test: file to modem audio, through noise, echo, clipping and a sample-rate offset, back to a file. Replace the SNR column above with measured numbers.
3. JNI and an Android send path (speaker) and receive path (microphone, Unprocessed source), with a lead-in tone for the transmitter's VOX and a level test tone.
4. Interface in three tabs: Record, Live (push to talk), Files (send, receive).
5. Phone to phone across a room, then through a radio, then against the FreeDV program on a PC.
6. Later: cable or USB audio, two-way resend.

## Next step 2: offline transcription

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
