# Codec2 Recorder plan

Last updated: 2026-11-15 (RADE messages built, 2.8)

## Start here

**Review and big plan for both apps: [ROADMAP.md](ROADMAP.md)** (2026-11-15). It is a proposal waiting for the owner's decisions (its section 2); nothing in it is built. It covers the code structure, the sound chain, the interface, the radio link and FlatRaster's picture. Its section 9 also names which model does which stage (Sonnet for test-guarded work, the stronger model for measurements and design). Until the owner decides, the lists below stand.

**Waiting for the owner: phone test of 2.8** (`dist/Codec2Recorder-2.8.apk`, installs over 2.7). 2.8 adds RADE voice messages (stage 8, built 2026-11-15) to the DRED recordings of 2.7. Nothing that touches Android audio has run on a phone: recording, playback, saving WAV, compressing files, radio sending, and all of RADE.

RADE phone test (stage 8):

1. RADE tab: RECORD MESSAGE, say a sentence, STOP. PLAY, then HEAR AS RADE. Is HEAR AS RADE understandable? Does it stutter? (On a computer RADE decodes 30 times faster than real time; a phone is not measured.)
2. Two phones, no radio: phone A SEND at about three quarters media volume, phone B LISTEN with its microphone 5 to 30 cm from A's speaker. Does B show "Signal locked" and an SNR, and does a recording "rx_RADE_..." appear in B's list when A finishes? How does it sound?
3. Through a radio: A's speaker at the transmitter's microphone (or VOX), B or a FreeDV station listening. Does FreeDV (PC program, 2.x, RADE V1 mode) decode it? Does the phone decode a FreeDV station? Identify the station in plain voice or CW.
4. LISTEN with Play while listening on earphones.

Phone test list (stage 9, part B, step 10):

1. 2.7 installs over 2.6.
2. Record 30 s of Polish at each level (best, balanced, small, smallest), with and without Denoise and Voice activation. Play, seek, pause.
3. Does playback stutter? On which phone? (Speed on a phone, above all a 32-bit one, is not measured. On a computer one second decodes in 31 ms.)
4. Is the recording level right: not quiet, not distorted? Does the level bar behave? (The ceiling moved from 11000 to 18000 and playback is raised twice, not 3.5 times; see part B results.)
5. Save WAV, share, rename, delete. How long does SAVE WAV take for a 10-minute recording? (No progress is shown.)
6. Recording with the screen off for ten minutes.
7. Compress an existing audio file (COMPRESS AUDIO), for example a WhatsApp voice note.
8. Send a recording phone to phone in Robust; it should join the list on the other phone and play. (Both phones need 2.7.)
9. Which level would you use every day? Is q12 (smallest) still good on your own voice?
10. With Denoise on and the default settings, does speech sound processed or pumping? On a computer the automatic level costs intelligibility with DRED (part B results); if you hear it, try SETTINGS: Rec level off auto, Compressor off.

Live voice with DRED over the 700D modem (part C) is dropped: RADE does it better and FreeDV stations can hear it (stage 8). Next, no phone needed: real-time RADE talk (push to talk, with earphones), or FlatRaster over radio (stage 6). After the phone test: stage 10.

### For the next session, in order

1. **Owner's phone and radio test of 2.8**, lists above and stage 1. Fix what it shows before building more.
2. **Real-time RADE talk** (push to talk), now that record-then-send exists: speech in and modem sound out at once, so it needs earphones on the receiving side and the radio's VOX or a cable on the sending side (stage 4). Build only after the phone test shows RADE works speaker to microphone.
3. **FlatRaster**: give it RADE messages too, or send `.frv` files over the DATAC modes (stage 6). Its own plan is `PLAN.md` in `apk-codec2flatraster`.
4. **Stage 10** after the phone test: write the confirmed DRED and RADE settings into `CLAUDE.md`; decide the name on screen (still "Codec2 Recorder" with no Codec 2 recording in it).
5. **Old branches**: `ccr-02a3eade-8um8m4`, `ccr-2585fd19-vajrqa`, `claude/eager-brahmagupta-wg61pp`, `claude/tender-cray-gfs8aa` and `claude/modest-noether-2xx3jl` are all in `main` or moved to other repositories. Delete them only with the owner's permission.

- `main` holds everything up to 2.8 (2026-11-15). Before stating what exists, still fetch and list all branches.
- Current release: 2.8, in `dist/`. 2.6 was never tested on a phone either (stage 1); its phone test is folded into the one above.
- FlatRaster (apk-codec2flatraster) has the same DRED voice and sound settings since its 0.15.
- The session needs the Android SDK (`bash setup.sh`) and the signing keys to deliver an APK. Without them it can still do the computer steps.

## Roadmap

Each stage ends with something that can be checked. Stages 1 to 3 need the owner's phone and radio; the rest can be prepared on a computer.

| # | Stage | Done when | State |
|---|---|---|---|
| 0 | Radio link built and measured on a computer (2.5, 2.6) | Files and live voice pass simulated channels | Done 2026-11-12 |
| 0a | Project records: every session starts from the current state | `main` holds the current version, and `CLAUDE.md` says where the project is | Done 2026-11-15 except deleting the old branches, which waits for the owner's permission |
| 1 | First phone test of 2.6 | 2.6 installs over 2.4, recording behaves as in 2.4, and a short recording crosses a table from phone to phone in Robust | Waiting for the owner |
| 2 | Fix what the phone shows | A file passes phone to phone in all three speeds; live voice is understandable both ways | Not started |
| 3 | Through a real radio | A file and live voice pass radio to radio; live voice passes between two phones (it is DRED from 2.8, so the FreeDV program on a PC will not decode it) | Not started |
| 4 | Cable instead of air | Send and receive through a headset cable or USB audio, with transmitter keying | Not started |
| 5 | Two-way link | Receiver reports missing frames, sender resends only those; short text messages | Not started |
| 6 | FlatRaster over radio | The same link in apk-codec2flatraster, with a low-bitrate Radio preset | Not started |
| 7 | Side tracks, any time | Offline transcription; background music removal. RNNoise is ruled out (owner, 2026-11-15) | Plans below |
| 8 | RADE: natural-sounding voice, live and as a saved message | RADE measured on a computer through the same simulated channels as 700D, and a decision made | RADE V1 measured 2026-11-15, better than 700D in every channel; record-then-send voice messages and listening built in 2.8. Waiting for the phone and radio test. Real-time talk not built |
| 9 | DRED replaces Codec 2: recordings (2.7, released 2026-11-14), then live voice (2.8) | Part A: measured on a computer. Part B: the app records, plays, exports and sends `.dred`, and Codec 2 is gone. Part C dropped for RADE (stage 8) | Part A done 2026-11-14 and the owner said go. Part B built and checked on a computer 2026-11-14, test build 2.7-test1 waiting for the phone test. Part C: the modem was shown to carry bytes on a computer; nothing built |
| 10 | After 2.7 is confirmed on the phone | DRED settings written into `CLAUDE.md`; app name decided | Not started |

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

#### Results, 2026-11-15

The owner approved downloading and running RADE. Source: `freedv/rade_c` at c8a3dc1 (2026-10-07), the FreeDV project's C port, BSD 2-clause, in `third_party/rade_c` (V1 files only, unchanged; 47 MB of source, most of it network weights). Its README says V2 is not for use on air yet, so V2 is stubbed out (`app/jni/rade_v2_stub.c`), which saves about 3 MB per processor type.

- Built against our Opus 1.6.1, not the Opus commit rade_c uses. One catch: Opus 1.6.1 sizes a convolution buffer in `dnn/nnet.c` for 1024 values and RADE's encoder needs more (found with AddressSanitizer: "buffer overflow detected"). rade_c's own build patches that line; `radeflags.sh` builds a patched copy (2048) and leaves `third_party/opus-1.6.1` unchanged. DRED output is byte-identical with the copy (`JT`).
- One library for both: `libvoice.so` with DRED, RADE V1 and one copy of Opus, 4.9 to 5.0 MB per processor type. APK 14 MB.
- The app's layer (`app/jni/radejni.c`, `Rade.java`) gives sample-identical modem sound and decoded speech to rade_c's `rade_tx_wav` and `rade_rx_wav` on 65 s of speech (`tests/dred/RA.java`). Getting there needed the same input clamping as the tool (a -32768 sample changed everything after it).
- RADE V1 against Codec 2 700C in FreeDV 700D, `tests/rade/compare.py`, 65 s of English speech, STOI against the original, mean of 3 noise seeds (lowest to highest), scored in 5 s pieces each aligned on its own:

| Channel | RADE V1 | Codec 2 700C in 700D |
|---|---|---|
| Clean | 0.85 | 0.73 |
| Noise, SNR 5 dB | 0.84 | 0.73 |
| Noise, SNR 2 dB | 0.83 | 0.73 |
| Noise, SNR 0 dB | 0.81 | 0.35 (0.17 to 0.73) |
| Noise, SNR -2 dB | 0.58 (0.17 to 0.79) | 0.33 (0.16 to 0.67) |
| Noise, SNR -4 dB | 0.75 | 0.14 |
| HF fading, SNR 5 dB | 0.76 (0.61 to 0.83) | 0.47 (0.17 to 0.62) |
| HF fading, SNR 2 dB | 0.53 (0.19 to 0.81) | 0.44 (0.17 to 0.59) |
| Speaker to mic, close, distorted, clocks 100 ppm apart, SNR 15 dB | 0.85 | 0.73 |
| Speaker to mic, echo 5 dB below direct, SNR 10 dB | 0.83 | 0.73 |
| Speaker to mic, echo as loud as direct | 0.60 (0.21 to 0.80) | 0.11 |

  Near its limit (-2 dB, fading at 2 dB, heavy echo) RADE either decodes a whole run well or fails for the whole run; that is why those cells range so widely. -4 dB came out better than -2 dB with these three seeds; more seeds would be needed to say where the edge is. RADE's decoded voice comes out about 0.4 s early in rade_rx_wav (the tool trims its start-up), so scoring has to search both ways.
- Speed on this computer with the app's generic flags: coding 210 times faster than real time, decoding 30 times. Not measured on a phone.
- The receiver reports the sender's end-of-over frame (checked with noise after the message), which is how LISTEN knows a message has ended.

Built in 2.8, RADE tab:

- RECORD MESSAGE: microphone through the recording voice chain and settings, up to 60 s, kept in the app's cache.
- PLAY (as recorded) and HEAR AS RADE (coded and decoded inside the phone).
- SEND: half a second of coded silence (opens a VOX), the message, RADE's end-of-over. Only the speaker runs, so no feedback.
- LISTEN: microphone at 8 kHz (Unprocessed where the phone has it), RADE decoded as it arrives; each message (ended by end-of-over, or 3 s after the signal is lost) is saved to the recordings as `rx_RADE_..._DRED-best.dred`. Play while listening plays it at once, for earphones.
- The 700D live-voice code (Codec 2 over 700D) is removed; the modem library keeps 700D for the tests.

Not done: real-time talk (RADE needs speech in and modem sound out at once; with earphones and a radio's VOX it could work), a waveform check against a real FreeDV station, speed on a phone. The LISTEN logic that ends a message is only exercised through its parts on a computer (`RA` with noise after the message), not as a whole.

#### Steps (as planned on 2026-11-14)

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

Listening page for the owner (private link): https://claude.ai/artifact/TcpmJS1rtvv3UL88YYxxJJ. Ten-second clips of two Polish and two English voices at DRED q4, q7, q9, q12 and Codec 2 3200, 1300, 700C, two noisy cases and the non-speech sound.

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

#### Owner's decisions, 2026-11-14

- The owner listened to the page and judged DRED good down to q12, and Codec 2 obsolete beside it. Go for the app.
- **Codec 2 is removed from the app completely**, in the same version that brings DRED. No Codec 2 recording, no Codec 2 modes on screen, and the app no longer opens `.c2` files.
- **Live voice moves from Codec 2 to DRED**, carried by the FreeDV modem (part C).

What follows from that, so nobody is surprised:

- Old `.c2` recordings will not play in 2.7. Whoever wants to keep one must save it as WAV with 2.6 before upgrading. The files themselves are not deleted.
- Android does not install an older version over a newer one. Going back from 2.7 to 2.6 means uninstalling, which deletes the recordings stored in the app. So 2.7 is first delivered as a test build that installs beside 2.6 (step 8).
- "Codec 2" here means the voice codec. The FreeDV modem library in `third_party/codec2-1.2` comes from the same project and stays, because the radio link is built on it.
- The settings in `CLAUDE.md` "confirmed good in 2.4" are Codec 2 settings. They stop applying. New DRED settings have to be confirmed on the phone, and `CLAUDE.md` rewritten then.
- Live voice will no longer be heard by the FreeDV program on a PC or by any other station's software. It becomes a mode between two copies of this app.
- The recogniser disagreed with the owner about q12 (88% of words wrong), and the Polish clips were synthetic. q12 is offered; the default is q9 until q12 has been heard on real recordings from the phone.
- The app is still called "Codec2 Recorder". The package `pl.dyktafon` must stay so it installs over older versions; the name on screen is the owner's to change.

#### Part B: DRED replaces Codec 2 in the app (version 2.7)

Written to be carried out by a new session with no memory of this one. Read `CLAUDE.md` first.

Outcome: the app records, plays, exports and sends `.dred` recordings. Codec 2 is gone. Sending files over the radio works as in 2.6. The Live tab is hidden until part C.

Decisions already made:

- DRED on its own, one block per second, fixed quality level, as in `tests/dred/dredtest.c` (`enc2` and `dec2`). That file is the reference: the JNI layer is the same calls.
- Four levels in the app, named by what the user gets, not by q:

| Name in the app | q | About, per minute | Note |
|---|---|---|---|
| DRED best | 4 | 10 kB | Clearest |
| DRED balanced | 7 | 6 kB | |
| DRED small | 9 | 4 kB | Default |
| DRED smallest | 12 | 2 kB | Owner found it good on the test clips; a recogniser did not |

What a new session needs:

- Android SDK and NDK: `bash setup.sh` (about 3 GB; if the download hosts are blocked, say so and stop before step 5).
- Signing: `c2.keystore` and `signing.env` from the private repository `apk-signing-keys`, folder `apk-codec2-recorder`. Without them, do the computer steps and hand over no APK.
- Opus 1.6.1 source: `http://archive.ubuntu.com/ubuntu/pool/main/o/opus/opus_1.6.1.orig.tar.xz`, sha256 `001ac615b9f8f8994b229ad2b1a5969ed12ab48b0d154d0af1016a5899cd6ba1`. The Opus project's own sites were blocked in the session that wrote this; the Ubuntu mirror worked.

Steps. Each ends with a check; do not go on while a check fails.

1. **Opus sources into the repository.** Put the release in `third_party/opus-1.6.1`, upstream files unchanged. Only these are needed: `include/`, `celt/`, `silk/`, `src/`, `dnn/*.c` and `dnn/*.h`, the four `*_sources.mk` files, `COPYING`. Leave out `dnn/torch`, `dnn/training_tf2`, `doc`, `tests`, and the weight files DRED does not use (`bbwenet_data.c`, `lace_data.c`, `nolace_data.c`). The files that get compiled, with their headers, are about 10 MB. Add `COPYING-opus` at the top level and a line in `README.md` under Licences (BSD, three clauses).
   Check: `tests/dred/build.sh` works against this folder (change it to build from the folder directly, as in step 2, so it no longer needs a separate Opus build).

2. **Build without autotools.** The source lists are in the `.mk` files: `OPUS_SOURCES`, `OPUS_SOURCES_FLOAT`, `CELT_SOURCES`, `SILK_SOURCES`, `SILK_SOURCES_FLOAT`, `DEEP_PLC_SOURCES`, `DRED_SOURCES` (158 files). Flags, tried on a computer on 2026-11-14 with gcc:
   `-O2 -DOPUS_BUILD -DVAR_ARRAYS -DHAVE_LRINT -DHAVE_LRINTF -DFLOAT_APPROX -DENABLE_DRED -DENABLE_DEEP_PLC -DENABLE_RES24 -DDISABLE_DEBUG_FLOAT -I. -Iinclude -Icelt -Isilk -Isilk/float -Idnn -Isrc`
   No `config.h`, no processor-specific source files. This gave a 2.5 MB program. On that computer it coded 145 s of speech in 1.0 s and played it in 5.5 s. A file made by this build played correctly on the autotools build and the other way round (same STOI), so files made on one processor type should play on another; check that again in step 9.
   Put the flags in one place, like `fdvflags.sh`.
   Check: the plain build of `dredtest` passes `python3 tests/dred/run.py`, with numbers close to the table in Part A. (`run.py` compares with Codec 2 through `tests/dred/c2rt`; keep `third_party/codec2` in the repository for that comparison until step 6 is done, then delete both.)

3. **File format `.dred`.** Fixed 8-byte header, then blocks.

   | Bytes | Meaning |
   |---|---|
   | 0 to 3 | `44 52 45 44` ("DRED") |
   | 4 | Format version, 1 |
   | 5 | DRED bitstream version of the library, `DRED_EXPERIMENTAL_VERSION`, 12 in Opus 1.6.1 |
   | 6 | Quality level q, 0 to 15 |
   | 7 | Frames of 20 ms per block, 50 |
   | then, repeated | Block length, 2 bytes, big endian, then that many bytes. Length 0 means one block of silence |

   Length in seconds is the number of blocks. A player that meets a bitstream version it does not know must say so and not play noise. Seeking goes to a block, so to the second; build the list of block positions when the file is opened.
   Write `Dred.java` in place of `C2.java`: header, `readHeader`, block index. Pure Java, no Android, so it runs in a computer test.
   Check: `tests/t6/` round trip: make a file from blocks, read it back, seek, reject a wrong magic and an unknown version.

4. **Native layer.** `app/jni/dredjni.c`, from `enc2` and `dec2` in `tests/dred/dredtest.c`:
   - coder: open(q), feed one 20 ms frame of 320 samples, `block()` after every 50 frames returning the bytes, close. On stop, feed silence until the block is complete and then two more frames, as `enc2` does.
   - player: open (starts the Opus decoder with one silent packet and complexity 5, see `start_decoder`), `decode(block)` giving 16000 samples, close.
   Build `libdred.so` in `build.sh` for arm64-v8a, armeabi-v7a and x86_64 with the 16 KB page flag, as `libc2.so` is built now.
   Check on a computer first: a host build of the JNI library, driven from Java like `tests/radio/RT.java`, gives the same `.dred` blocks and the same sound as `dredtest` for one speech file.

5. **Recording at 16 kHz.** Rewrite the loop in `RecService` for DRED.
   - `AudioRecord` at 16 kHz, same audio source and device choice as now. Keep VOICE_RECOGNITION as the default source: that finding was about the microphone, not the codec.
   - Voice chain: `VoiceChain`, `Denoiser`, `Vad` and `Limiter` are written for 8 kHz. Move them to 16 kHz: filter coefficients, frame sizes, the denoiser's bands. A saved noise sample from 2.6 is for 8 kHz and must be discarded.
   - Levels: `CEILING`, `AUTO_PEAK` and the playback boost exist because the Codec 2 decoder overshoots peaks. On the test speech DRED's decoded peak was 1.00 times the input peak (99.9th percentile 0.94). Start with a ceiling near 23000 and no playback boost, and measure.
   - Measure the 16 kHz chain with `tests/dred/run.py` on noisy input, with and without the denoiser, because noise costs DRED a lot.
   - Voice activation: gate the sound before the coder, not the coded blocks. Keep 400 ms of sound as pre-roll and feed it when the gate opens.
   - Pause: stop feeding the coder.
   - File name `stamp_DRED-small.dred` and so on.
   Check: computer test that feeds a speech file through the recording path (without `AudioRecord`) and plays the result back with `dredtest dec2`.

6. **Playing, the list, and taking Codec 2 out.** `MainActivity` handles `.c2` in `play`, `list`, `saveWav`, `compress`, `importFile`, `rename`, `share`, `saveC2`, and `RadioService` decides whether a received file is a recording. Change all of these to `.dred`.
   - Play at 16 kHz through `AudioTrack`; decode one block ahead on the playback thread.
   - `list`: level name from the header, seconds from the block count. A `.c2` file found in the folder is listed as "Made by an older version, can't be played here" with share and delete only, so nothing vanishes silently.
   - Save WAV at 16 kHz. `saveC2` becomes save `.dred`.
   - Import and "open with" accept `.dred` by its header, and refuse `.c2` with a plain sentence.
   - "Compress existing audio files" writes DRED: resample the source to 16 kHz.
   - Radio: a received file with a valid DRED header joins the recordings list. Time on air already comes from the file size.
   - Remove `C2.java`, `app/jni/c2jni.c`, `libc2.so` from `build.sh`, `third_party/codec2` (1.0), `COPYING-codec2` only if nothing of that licence is left (the modem library is LGPL too, so it stays), and the Codec 2 tests `tests/c2test.c`, `tests/t*` that test the codec.
   - `RadioService.live` and the Live tab: hide in 2.7. Take `FREEDV_MODE_700E_EN` out of `fdvflags.sh` if part C will not use it; keep 700D, which part C needs.
   Check: `node tests/ui_radio.js` updated and passing; `tests/radio/run.py` still passes for files; `grep -ri codec2 app/` finds only the modem.

7. **Page.** In `app/assets/index.html`, `MODES` becomes the four DRED levels with size per minute, default DRED small. A stored choice of a Codec 2 mode falls back to the default. Live tab hidden. Interface text in English.

8. **Build and deliver, in two moves.**
   - First a test build that installs beside 2.6: a different application id (`aapt2 link --rename-manifest-package pl.dyktafon.test`) and a different `FilesProvider` authority, or the install fails. Label it "Recorder test". The owner tries it without risking the recordings in 2.6.
   - After the owner's go: the real build, `versionCode` 18, version name 2.7, signed with the real key, in `dist/` in place of 2.6.
   - Update `README.md` (what DRED is, sizes, that only this app plays `.dred`, that `.c2` files no longer play and how to save them as WAV with 2.6 first, licences), `CLAUDE.md` (two libraries are now `libdred.so` and `libfdv.so`; the 2.4 settings paragraph replaced by "DRED settings not yet confirmed on a phone") and this plan.
   - Say plainly what was not run on a phone: everything that touches Android audio.
   Expect about 2.5 MB more for each of the three processor types, less what `libc2.so` took.

9. **Computer checks before handing over.**
   - A file coded by the x86_64 library plays on a second build made with different compiler settings.
   - A 30-minute recording: file size, no drift in length, memory steady.
   - Speed with the generic build flags, as an early warning for slow phones.

10. **Phone test by the owner**, on the test build. Put this list at the top of the plan when it is delivered:
   - The test build installs beside 2.6 and both work.
   - Record 30 s of Polish at each level, with and without Denoise and Voice activation. Play, seek, pause.
   - Does playback stutter? On which phone? (Speed on a phone, above all a 32-bit one, is not measured.)
   - Is the recording level right: not quiet, not distorted? Does the level bar behave?
   - Save WAV, share, rename, delete.
   - Recording with the screen off for ten minutes.
   - Send a recording phone to phone in Robust; it should join the list on the other phone and play.
   - Which level would you use every day? Is q12 still good on your own voice?

Time on air for a 60 s recording with +50% repair, calculated from the burst times measured in stage 0, not measured for these files:

| Recording | Size | Fast | Robust | Very robust |
|---|---|---|---|---|
| Codec 2 700C, for comparison | 6.0 kB | 87 s | 4.4 min | 17 min |
| DRED small (q9) | 3.8 kB | 53 s | 2.7 min | 10.6 min |
| DRED smallest (q12) | 2.1 kB | 35 s | 1.6 min | 6 min |

Risks to watch:

- Speed on old and 32-bit phones. If playback cannot keep up, decode ahead into a buffer before starting to play.
- The DRED format is experimental in Opus 1.6.1. The app must keep this exact library for as long as `.dred` files with bitstream version 12 exist; a newer Opus goes in beside it, not instead of it.
- A neural player can produce clean-sounding wrong words when the input is noisy. Keep the denoiser measurement in step 5.
- With Codec 2 gone there is no second format to fall back on. That is why the test build comes first.

#### Part B results, 2026-11-14

Built: 2.7-test1, `dist/Codec2Recorder-2.7-test1.apk` (package `pl.dyktafon.test`, "Recorder test"). Nothing was run on a phone. All checks below are on a computer (2.1 GHz Xeon, x86_64).

| Step | What was done | Check |
|---|---|---|
| 1 | `third_party/opus-1.6.1`: `include`, `celt`, `silk`, `src`, `dnn/*.c`, `dnn/*.h`, the `*_sources.mk` files, `COPYING`. Left out the folders and weights the plan lists, and `celt/tests`, `silk/tests`. 12 MB. `COPYING-opus`, licence line in `README.md` | `tests/dred/build.sh` builds from this folder |
| 2 | `opusflags.sh`: the flags from the plan and the 158 files read from the `.mk` files | `run.py` on 8 English files (no Polish speech could be fetched): kB per minute 10.0, 6.0, 3.8, 2.2 at q4, 7, 9, 12; STOI 0.87, 0.84, 0.81, 0.75. Close to part A |
| 3 | `Dred.java`: header, `readHeader`, block index; a block cut short at the end is left out, so a recording survives the phone dying | `tests/t6/DT.java`: round trip, seek, cut-off file, wrong magic, unknown format and bitstream version, damaged header and block, old `.c2` |
| 4 | `app/jni/dredjni.c`, `DredLib.java` (coder with the end-of-recording flush, player). `libdred.so` in `build.sh` for the three processor types: 2.4 to 2.5 MB each. NEON is on for both ARM types by default | `tests/dred/JT.java`: the JNI library gives byte-identical blocks and identical sound to `dredtest` at q4, q9, q12 |
| 5 | `VoiceChain`, `Denoiser`, `Limiter`, `Compressor` take the sample rate: same corners and time constants as 2.4, so at 8 kHz they are the 2.4 chain (live voice still uses that). Denoiser window 512 at 16 kHz, so the same 31 Hz bands. Chain frames 40 ms, as 700C had. `Recorder.java`: chain, voice activation gating sound with 400 ms of sound as pre-roll, coder, file; used by `RecService` and file compression. `Resampler` rewritten for any rate up or down. The 8 kHz noise sample from 2.6 is deleted at start | `tests/dred/RS.java` (resampler), `RP.java` (recording path without Android) and `chain.py`, below |
| 6 | `MainActivity`, `RadioService`: play at 16 kHz with a 2 s buffer (one block ahead), seek by the second, list with level and length from the header (cached), save WAV streamed, save `.dred`, import by header, compress to DRED. `.c2` files listed as "Made by an older version" with share and delete only; playing or importing one gives a sentence about 2.6. A received file with a DRED header joins the list. Removed `C2.java`, `c2jni.c`, `third_party/codec2`, `tests/c2test.c`, `tests/t`, `t2`, `t3`, `t4` (8 kHz chain and Codec 2 experiments that no longer compiled), `tests/dred/c2rt.c`. 700E out of `fdvflags.sh`. `COPYING-codec2` stays for the modem | `node tests/ui_radio.js` passes; `tests/radio/run.py` files part: the same as in 2.6 in every row, except HF fading at 0 dB in Very robust, 2 of 3 against 1 of 3 (run to run variation; the data modes were not changed), and nothing decoded from 10 minutes of noise; `grep -ri codec2 app/` finds only the modem, the app name and the old-file handling |
| 7 | Page: four DRED levels, default small; a stored Codec 2 mode falls back; Live tab hidden; OPEN .DRED, SAVE .DRED | `ui_radio.js` |
| 8 | `bash build.sh test`: package `pl.dyktafon.test`, provider `pl.dyktafon.test.files`, label "Recorder test", version name 2.7-test1. Manifest raised to `versionCode` 18, 2.7 for the real build. `README.md`, `CLAUDE.md` | Built and signed with the real key; installs beside 2.6 by package name (not tried on a phone) |
| 9 | Computer checks | Below |

The recording path, `tests/dred/chain.py`, DRED q9, 8 English files with one second of background before and after. STOI against the clean original:

| Input | DRED alone | Recording path, Denoise off | Recording path, Denoise on |
|---|---|---|---|
| Clean | 0.81 | 0.72 | 0.72 |
| Clean, 20 dB quieter | 0.80 | 0.74 | 0.76 |
| Pink noise 10 dB below the speech | 0.70 | 0.67 | 0.69 |
| The same, 20 dB quieter | 0.70 | 0.69 | 0.71 |
| White noise 5 dB below the speech | 0.62 | 0.59 | 0.62 |

- The denoiser helps a little in noise and never hurts. Noise still costs DRED a lot, as in part A.
- The chain itself costs about 0.08 on clean speech. Switching parts off one at a time (clean input, Denoise off): all defaults 0.71; treble off 0.73; lift off 0.72; compressor off 0.74; gate off 0.72; rumble cut at 40 Hz 0.72; all of them off 0.76; all off with fixed gain instead of automatic level 0.81. So the largest single cost is the automatic level, which follows syllable peaks (fall 0.4 per 40 ms frame, rise 0.04). These are the settings confirmed on the phone in 2.4 for Codec 2, so they were kept. STOI against the clean original also counts intended changes (treble, compression) as loss; it is not a listening test. Decide after the owner has listened (phone test item 10). A gentler automatic level is the first thing to try if the owner hears pumping.
- Levels: with the limiter ceiling at 22000 (the plan's starting point) and at 20000, one onset in 80 recordings came out of the decoder at 1.5 times the ceiling and hit full scale. At 18000 none did; the highest decoded peak was 1.26 times the ceiling (22714), the median 0.95. So the DRED ceiling is 18000. Speech then sits at about -26 dBFS, so playback and SAVE WAV raise it twice (+6 dB) through the same look-ahead limiter 2.4 used at 3.5 times.

Step 9 checks:

- Another build: a `dredtest` built with `-O3 -march=native` (AVX2) makes different bytes (float rounding) but its files play on the app's build and the other way round with the same STOI (0.84 and 0.84, 0.64 and 0.64 on three files). Not tried: ARM builds, which need a phone.
- A 30-minute recording through the recording path (`RP.java`, 32 MB Java heap, input streamed): 1801 s coded from 1800 s (the last block is filled out on stop), 123 kB, 4.1 kB per minute; with voice activation 1684 s. No growth in memory. The chain and coder ran 121 times faster than real time.
- Speed with the app's generic flags, 30 minutes through the JNI library: coding 231 times faster than real time, playing 32 times (31 ms per one-second block). The autotools build in part A played 123 times faster because it adds AVX2 code for x86; on ARM the generic build already uses NEON. A phone core several times slower should still keep up; a slow 32-bit phone is the risk. SAVE WAV of 30 minutes on such a phone could take minutes, with no progress bar.

Not done in part B: real Polish speech (no source could be fetched), anything on a phone or on ARM, the "open with" route (the app has no such intent filter; only OPEN .DRED), and Vosk word counts (model not available here).

#### Part C: live voice with DRED over the FreeDV modem (version 2.8)

The owner asked whether the FreeDV modem can send DRED live. On a computer, yes. Tried on 2026-11-14 with `tests/dred/fdvpipe.c`: random bytes in place of Codec 2 voice through the 700D modem of `libfdv`, white noise added, SNR in 3 kHz.

| Channel | Frames intact, of 400 |
|---|---|
| Clean | 400 |
| 5 dB | 398 |
| 2 dB | 395 |
| 0 dB | 389 |
| -2 dB | 321 |

- 700D carries 14 bytes every 160 ms: 700 bit/s, 87.5 bytes per second. About 1.1 kHz wide.
- Almost every damaged frame was flagged by the modem (81 of 86 at -2 dB). Not all, so our own checksum is needed.
- 700E passed nothing in the same test. Not looked into.
- Not tried: fading, the speaker-to-microphone path, real DRED blocks.

Why it fits: DRED small averages about 63 bytes per second and each block stands on its own, so a lost block costs its own second and nothing after it.

Design to build and measure:

1. Block of 48 speech frames (0.96 s) in 6 modem frames, 84 bytes: 1 byte length, up to 81 bytes of DRED, 2 bytes CRC16. The receiver finds where a block starts by trying the CRC at each of the 6 positions, so no marker is sent.
2. Size control: code each block at the finest level from q8 to q15 that fits 81 bytes (`dred_encode_silk_frame` can be called again on the same sound with a coarser level).
3. A block with a bad CRC is played as silence at first; try the decoder's own gap filling second.
4. Expected whole blocks, from the table: about 97% at 5 dB, 93% at 2 dB, 85% at 0 dB, 26% at -2 dB. So about 0 dB is the limit, where Codec 2 voice reached -2 dB. If that matters: half-length blocks, or DRED smallest with repair frames in the spare room. Measure before choosing.
5. Delay: about one second for the block plus the modem.
6. Put it in `tests/radio/run.py` beside the file modes, with the channels from `chan.py`, and score it with STOI against the clean speech, not with the loudness contour.
7. Then `RadioService.live` and the Live tab: tap to talk as now, 16 kHz voice chain from part B.
8. If the speaker-to-microphone path proves too rough for 700D on the phone, fall back to sending the blocks through the Fast or Robust data mode. That is exact, but 4 to 6 seconds late.

On the air this is a mode no other program decodes. Publish its description in this repository, and identify the station in plain voice or CW (stage 3).

### Stage 10: after 2.7 is confirmed

1. `CLAUDE.md`: write down the DRED settings the owner confirmed on the phone.
2. Decide the app's name on screen.
3. Stage 3's "live voice is heard by the FreeDV program on a PC" no longer applies; replace it with "live voice passes between two phones through two radios".

## Status

| Part | State |
|---|---|
| Record, pause, play, seek, rename, delete | DRED in 2.7-test1: built, not yet run on Android |
| Codec 2 modes from 3200 down to 450 | Removed in 2.7. `.c2` files are listed but not played |
| Recording with the screen off | Built, not yet confirmed on Android |
| Share, save .dred, save WAV at 16 kHz, open .dred | Built in 2.7-test1, not yet run on Android |
| Denoise, noise sample, continuous adaptation | Tested on synthetic speech with added noise |
| Voice activation with 400 ms pre-roll | Tested on synthetic speech with added noise |
| Microphone choice, rec level, audio source, treble boost, voice sensitivity and hold, denoise strength | Built, waiting for phone test |
| RNNoise comparison | Ruled out. The owner used RNNoise in another project (ai-film-lab) and found the sound awful (said 2026-11-15; this file had only recorded it as blocked). Version 0.2 (2024) has a larger, retrained model and is reported to keep the voice clearer than 0.1, but it still applies one gain per frequency band, which is what dulls a voice in changing noise. Not looked at here. Don't propose it again unless the owner asks |
| Compress existing audio files through the voice path | DRED, any source rate through the new resampler (tested on a computer with tones), waiting for phone test |
| Rumble cut and consonant lift | Measured on synthetic speech, waiting for phone test |
| Look-ahead limiter and noise gate | Measured on synthetic speech, waiting for phone test |
| Bluetooth microphones | Built, not tested: no Bluetooth headset available in testing |
| Remove background music | Not started, plan below |
| Radio: sending and receiving recordings as files, with repair frames (2.5) | Modem and transport tested on a computer through simulated channels. Android audio part built, not run anywhere |
| Radio: live voice, FreeDV 700D (2.5) | Modem tested on a computer. Android part built, not run anywhere. Hidden in 2.7; 700E no longer built |
| Record, Live and Files tabs (2.5) | Page tested against a stub of the app, not on a phone |
| DRED recordings (stage 9) | In the app from 2.7-test1. Format, JNI library, recording path, resampler, levels, 30-minute run and cross-build playback checked on a computer. Nothing run on a phone |
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
