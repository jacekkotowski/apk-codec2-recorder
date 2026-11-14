# Codec2 Recorder plan

Last updated: 2026-11-14

## Roadmap

Each stage ends with something that can be checked. Stages 1 to 3 need the owner's phone and radio; the rest can be prepared on a computer.

| # | Stage | Done when | State |
|---|---|---|---|
| 0 | Radio link built and measured on a computer (2.5, 2.6) | Files and live voice pass simulated channels | Done 2026-11-12 |
| 0a | Project records: every session starts from the current state | `main` holds 2.6, and `CLAUDE.md` says where the project is | Proposed 2026-11-14, waiting for the owner's go-ahead |
| 1 | First phone test of 2.6 | 2.6 installs over 2.4, recording behaves as in 2.4, and a short recording crosses a table from phone to phone in Robust | Waiting for the owner |
| 2 | Fix what the phone shows | A file passes phone to phone in all three speeds; live voice is understandable both ways | Not started |
| 3 | Through a real radio | A file and live voice pass radio to radio; live voice is heard by the FreeDV program on a PC | Not started |
| 4 | Cable instead of air | Send and receive through a headset cable or USB audio, with transmitter keying | Not started |
| 5 | Two-way link | Receiver reports missing frames, sender resends only those; short text messages | Not started |
| 6 | FlatRaster over radio | The same link in apk-codec2flatraster, with a low-bitrate Radio preset | Not started |
| 7 | Side tracks, any time | Offline transcription; RNNoise comparison; background music removal | Plans below |
| 8 | RADE: natural-sounding voice, live and as a saved message | RADE measured on a computer through the same simulated channels as 700D, and a decision made | Not started. Running the downloaded code needs the owner's approval |
| 9 | DRED recordings: record, play and send a neural format smaller than 700C | Part A: a DRED file made and played on a computer, with size and listening results. Part B: the app records, plays, exports and sends it, confirmed on the phone | Part A measured on a computer 2026-11-14, waiting for the owner to listen. Part B not started |

### Stage 0a: project records

Why: on 2026-11-14 a session started from `main`, which held only the first import commit and a plan saying the radio transfer was "Not started". The 2.5 and 2.6 work was on an unmerged branch. The session answered from the old plan. Sessions keep no memory of earlier ones, so the files must carry the state.

1. Bring `main` up to date with the 2.6 work, through a pull request or a direct push with the owner's permission.
2. Add a "Where we are" section to `CLAUDE.md`: current version; that "radio link" means the FreeDV modems (700D, 700E, DATAC1, DATAC3, DATAC4), not HamDRM; the sister repositories (apk-codec2flatraster, Heart Sounds, apk-signing-keys).
3. Add two rules to `CLAUDE.md`: before stating what exists in the project, fetch and list all branches, because the newest work may be unmerged; end every session by updating `PLAN.md` and getting the work into `main`.
4. Check the two leftover branches `ccr-02a3eade-8um8m4` and `ccr-2585fd19-vajrqa` (their last commits say the work moved to other repositories), then delete them with the owner's permission.

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
- Pack recordings tightly before sending. A `.c2` file pads each frame to whole bytes (700C: 28 bits in 4 bytes, 450: 18 bits in 3 bytes), so the sent size could drop 12.5% for 700C and 25% for 450. Not built.
- Frames are rounded up to whole bursts of four. On the 2.6 kB test in Fast this turned +50% repair into +100% (6 file frames, 12 sent). Check whether the modem allows a shorter last burst.

### Stage 8: RADE

RADE is the FreeDV project's newer voice mode: a neural network turns speech straight into radio symbols, and a neural vocoder (FARGAN) rebuilds it. Looked at on 2026-11-14 from the project's pages and the source of the Android app `pepefrog1234/RADE_decode_Android` (app code LGPL 2.1, RADE and Opus BSD). Nothing below was run or measured here.

| | Our 700D / 700E | RADE V1 |
|---|---|---|
| Speech | Codec 2 700C, 4 kHz audio | Natural, 8 kHz audio |
| Width on air | 1.1 to 1.5 kHz | 1.5 kHz |
| Weak signal | Our simulation: 700D usable at -2 dB in noise | Published: about -2 dB |
| Library | About 1.3 MB per processor type | 3.9 MB for arm64, the only type that app builds |
| Speech sample rate | 8 kHz | 16 kHz in and out; the modem sound is 8 kHz like ours |
| Files | Yes, DATAC modes with repair frames | No: it sends values, not bits, with no checksum |

RADE V2 is in beta test: about 860 Hz wide and about 3 dB more sensitive by the project's simulations. The Android source already holds V2 code.

Open risks: whether RADE survives the speaker-to-microphone path (that app's README says it "works, but USB audio is far more reliable"); speed on older and 32-bit phones; V1 may be replaced by V2.

#### Saving RADE as a file

What goes on the air is 2000 numbers per second: 3 groups of 80 values in each 120 ms modem frame. Anything that can be sent can be saved. Three ways, by size per minute of speech (700C is 6 kB):

| What is saved | Size per minute | State |
|---|---|---|
| The modem sound as 8 kHz WAV | 960 kB | Works with existing code. Playing the file into a radio is a valid RADE transmission |
| The 2000 values per second, at 8 bits each | 120 kB | Our own format, not built |
| The same at 4, 2 or 1 bit each | 60, 30 or 15 kB | Our own format, not built, sound quality unknown |

Reason to expect small sizes can work: RADE is built to survive heavy noise, and at its weakest usable signal the radio channel cannot carry more than about 1.8 kbit/s (Shannon limit for 1.5 kHz at -2 dB in 3 kHz). So a coarse copy of the values should hold what a weak-signal listener hears. How it sounds at each size has to be measured. If it holds up, this is a recording format with natural sound at roughly the size of Codec 2 2400, and it could go through the file link as an exact copy.

Estimates for the coarse copies, not measured. The values are plain numbers with no fixed range (RADE V1 runs with `bottleneck = 3`, so the encoder's last layer is linear). If they are bell-shaped and the decoder treats rounding error like channel noise, the error at 1, 2 and 4 bits is 4.4, 9.3 and 20 dB below the signal. That is roughly like receiving RADE at 3, 8 and 19 dB SNR, where its floor is -2 dB.

At 4 bits (60 kB per minute) Opus at 8 kbit/s is the same size, already in Android, and playable anywhere, so only 1 and 2 bits are worth having. A file is tied to one set of network weights: a V1 file needs the V1 decoder for ever.

Possibly a better tool for small natural-sounding recordings: DRED, the speech coder inside Opus that RADE grew out of. It is built to produce bits and uses the same vocoder (FARGAN). From the Opus 1.5 release page, checked 2026-11-14: one second of speech is carried as redundancy for about 12 to 32 kbit/s extra across 50 packets per second, which works out to about 0.24 to 0.64 kbit/s, or 1.8 to 4.8 kB per minute. It adds about 2 MB to the library and is off unless Opus is built with `--enable-dred`. The same page warns that DRED is not yet standardized and the 1.5 version will not match the final one. It is designed to ride inside normal Opus packets as backup against lost packets, not as a codec of its own; whether it can write a standalone file is not checked. Its sources are in that repository's Opus tree, but the app builds with it off.

#### Steps

1. Owner approves running the downloaded RADE code on the computer.
2. Build the RADE library from that repository for the computer. Check its output against the app's own test vectors if it has them.
3. Put RADE into `tests/radio/run.py` beside 700D and 700E: the same channels, including speaker to microphone, echo and the 100 ppm clock difference.
4. Save-as-file experiment: store the 2000 values per second at 8, 4, 2 and 1 bit, decode each, and compare with unstored RADE and with Codec 2 at the nearest size. Include noisy speech and a non-speech sound, because a neural decoder can turn them into clean-sounding but wrong speech. Look at whether DRED can do the same job smaller. The owner listens; a number alone will not settle it.
5. Measure decode and encode speed on the computer, then estimate for arm64 and 32-bit ARM.
6. Decide: RADE as a third Live mode, as a "record, then send" voice message, as a recording format, or not at all. A build with RADE would need 16 kHz capture and playback added.

Do stage 1 (first phone test of 2.6) before building any of this into the app.

### Stage 9: DRED recordings

Goal: a new recording format beside the Codec 2 modes. DRED is the neural speech coder inside the Opus library (BSD licence). Expected size is 1.8 to 4.8 kB per minute, against 6 kB for 700C, with a natural-sounding voice that may not sound like the speaker. The owner accepts that as long as it is legible.

#### Part A results, 2026-11-14 (`tests/dred/`)

Done on a computer with Opus 1.6.1 (`opus_1.6.1.orig.tar.xz` from the Ubuntu source pool, sha256 `001ac615b9f8f8994b229ad2b1a5969ed12ab48b0d154d0af1016a5899cd6ba1`, built with `--enable-dred`). Nothing was run on a phone. The owner has not listened yet, so there is no go or no go.

How the file is made. Two ways were tried:

- Public interface only (normal Opus encoder with DRED on, keep one packet per second): rejected. The encoder only fills a whole second of DRED at very high bitrates. At 64 kbit/s it gave 7.5 to 8.8 kB per minute and STOI 0.68 to 0.71; at 16 to 32 kbit/s most of each second was missing.
- DRED on its own: chosen. The test calls the library's internal DRED encoder (`dred_compute_latents`, `dred_encode_silk_frame`) once per second with one fixed quality level q (0 finest, 15 coarsest), and the player uses `dred_ec_decode`, `opus_dred_process` and `opus_decoder_dred_decode`. No normal Opus sound is stored. The Opus sources stay unchanged; our code includes two internal headers.

Test speech: 8 real English voices (JFK from whisper.cpp, 7 radio amateurs from the RADE repository) and 3 Polish voices from the Piper sample set, which are synthetic. No real Polish speech was available here. 144 s in all. "Noisy" is the same speech with pink noise 10 dB below it. Codec 2 was fed the same sound cut to 8 kHz, without the app's voice chain.

Two measures, neither a listening test. STOI is a standard 0 to 1 estimate of intelligibility against the clean original. "Words wrong" is what a small offline English recogniser (Vosk small en-us) gets wrong against its own transcript of the clean original, 185 words, so differences of a few points mean nothing. The recogniser is much weaker than a person: cutting the uncoded sound to 4 kHz already costs it 17%.

| Coder | kB per minute, clean | kB per minute, noisy | STOI clean | STOI noisy | Words wrong, clean | Words wrong, noisy |
|---|---|---|---|---|---|---|
| DRED q0 | 15.6 | 13.4 | 0.88 | | 24% | 39% |
| DRED q4 | 9.7 | 7.7 | 0.88 | 0.79 | 28% | 49% |
| DRED q6 | 7.0 | 5.5 | 0.85 | | 35% | 61% |
| DRED q7 | 5.8 | 4.5 | 0.86 | 0.76 | 44% | 71% |
| DRED q9 | 3.8 | 2.7 | 0.83 | 0.73 | 52% | 74% |
| DRED q10 | 3.1 | 2.2 | 0.80 | | 58% | 86% |
| DRED q12 | 2.1 | 1.5 | 0.77 | 0.66 | 88% | 92% |
| DRED q15 | 1.5 | 1.1 | 0.72 | 0.60 | 100% | 96% |
| Codec 2 3200 | 24.0 | 24.0 | 0.84 | 0.79 | 37% | 56% |
| Codec 2 1300 | 10.5 | 10.5 | 0.79 | 0.72 | 63% | 73% |
| Codec 2 700C | 6.0 | 6.0 | 0.72 | 0.63 | 58% | 80% |
| Codec 2 450 | 4.5 | 4.5 | 0.66 | 0.56 | 86% | 85% |

STOI is over all 11 files where a noisy figure is given, and over the 8 English files in the other rows. DRED size depends on the sound; the table gives the mean, and single files were within about 15% of it.

What this says:

- At the size of 700C (about 6 kB per minute), DRED q7 scores better on both measures: 44% words wrong against 58%.
- DRED q10 matches 700C's word score at half the size (3.1 kB per minute).
- DRED q6 at 7 kB per minute matches Codec 2 3200 at 24 kB per minute.
- Below about 3 kB per minute (q12, q15) the recogniser understands almost nothing, although STOI still looks fair. This is the "clean but wrong" risk: the sound has the rhythm of speech with the wrong sounds in it. Do not offer those levels.
- Even the finest level loses a quarter of the words for this recogniser, so the neural voice itself costs something. Codec 2 3200 loses more.
- Noise hurts DRED about as much as it hurts Codec 2. The app's denoiser was not in this test.

Other checks:

- Joins between the one-second blocks: the spectral error in the 40 ms around a join was 2.4 to 3.8 dB, the same or lower than elsewhere (2.7 to 3.9 dB). Blocks of 0.2 s gave the same STOI and nearly twice the size, so one second is right.
- Level over 144 s: output 0.5 to 0.7 dB below the input at the start, middle and end. No fading.
- Delay through coder and player: 16 ms.
- Speed on a 2.1 GHz Xeon, one thread: coding 536 times faster than real time, playing 123 times. Not measured on a phone.
- Size: the test program with the DRED coder and player is 2.5 MB stripped.
- Non-speech (beeps over hiss): output 9 dB quieter than the input. Listen to it.
- Opus 1.6.1 still marks the DRED format as experimental (version 12 in `dnn/dred_config.h`), so the limit below about format changes stands.

Still open: the owner's listening test, above all on real Polish speech; speed on a phone; how DRED behaves after the app's voice chain.

Known limits:

- Only this app plays the files. An ordinary player ignores DRED. Sharing with others goes through export to WAV, as for `.c2`.
- The format is not final. The Opus 1.5 release page says its DRED will not match the standardized one. The file header must name the library version, and the app must keep that decoder for as long as such files exist.
- DRED works on 16 kHz speech. The app records at 8 kHz and `VoiceChain` is written for 8 kHz.
- A neural decoder can turn noise into clean-sounding but wrong speech.
- About 2 MB more per processor type, by the Opus project's figure.

#### Part A: on a computer

1. Owner approves downloading and running the Opus source. Done 2026-11-14.
2. Take an Opus release that includes the DRED weights. Done: 1.6.1, checksum above.
3. Write `tests/dred/`. Done: `dredtest.c` (coder and player), `c2rt.c` (Codec 2 for comparison), `run.py` (all measurements), `build.sh`.
4. Measure size and check the joins. Done, results above.
5. Compare with Codec 2. Done by STOI and by a recogniser for English. Not done: listening by the owner, real Polish speech.
6. Decide go or no go, and fix the settings. Waiting for the owner's listening. Proposal: offer two levels, q7 ("about the size of 700C, clearer") and q4 ("about the size of 1300, clearest"), and nothing coarser than q9.

#### Part B: in the app, only after a go and after stage 1

1. File format `.dred`: magic bytes, format version, Opus library version and its DRED version number, quality level, then one block per second with a 2-byte length before each. Seeking goes by block, so to the second.
2. Put the Opus 1.6.1 sources in `third_party/`, unchanged. Build `libdred.so` with the NDK for the three processor types, with the 16 KB page flag and DRED on, and a small JNI layer like `c2jni.c` built from `tests/dred/dredtest.c`.
3. Recording: add 16 kHz capture for this format only. The confirmed 8 kHz settings in `CLAUDE.md` stay as they are for Codec 2. Decide by measurement whether the voice chain runs at 16 kHz, or whether DRED gets a lighter chain.
4. Add "DRED" to the compression list, with its size per minute.
5. Player: play, seek, save WAV, share. Open `.dred` files from other apps.
6. "Compress existing audio files" can produce `.dred`.
7. Radio link: a received `.dred` joins the recordings list like a `.c2`. The time on air shown before sending comes from the file size, as now.
8. Tests on a computer, new version number, APK in `dist/`, then the owner's phone test.

Not part of this stage: live voice with DRED. It would be a mode nobody else can decode; see the RADE stage for live voice.

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
