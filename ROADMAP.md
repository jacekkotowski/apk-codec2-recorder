# Review and big plan: Codec2 Recorder and FlatRaster

Written 2026-11-15, at Codec2 Recorder 2.8 and FlatRaster 0.15. This is a proposal. Nothing in it is built, and nothing was changed in either app. `PLAN.md` in each repository still describes what exists; this file says where both apps should go and in what order.

Both repositories were read in full (all app code, native layers, pages, build scripts, tests, plans). No program was built or run for this review, so every number here is one of:

- **measured earlier**: taken from `PLAN.md`, with the place named;
- **from the code**: read in the source, with file and line;
- **estimate**: my own reasoning, to be measured before anything is built on it.

## 1. The short version

1. **The parts are good, the building is not.** The signal code (voice chain, file transport with repair frames, the `.dred` and `.frv` formats, the posterize filter) is careful, commented with reasons, and tested on a computer. Around it sit two 500-line activity classes that do everything, state passed through global variables, settings repeated in six places, and lines up to 540 characters wide. A person without an AI assistant would struggle to change it.
2. **The two apps are one app and a half, kept in step by copying.** Seven voice files, the DRED native layer and 12 MB of Opus source are copied between the repositories, and the copy has already drifted: FlatRaster holds the cleaner version of the settings, microphone and noise-sample code. Make it one repository with a shared core and two builds.
3. **Four releases have never run on a phone** (2.5 to 2.8, and FlatRaster 0.2 to 0.15). Every further feature rests on that. The plan below starts with a self-test built into the app, so one phone, with no second phone and no radio, can report most of what the owner's test list asks.
4. **The default sound processing makes DRED worse by the project's own measurement** (intelligibility score 0.81 without the chain, 0.72 with it; `PLAN.md` part B). The chain was tuned by ear for Codec 2. For a neural coder the rule should be: before the coder only remove what harms it (rumble, noise, wrong level); move treble, compression and consonant lift to playback, where they cost nothing and can be changed afterwards.
5. **The interface is built around the technology, not the task.** For the real use (phone held at the microphone of a handheld, CB or HF radio, no grid) the missing things are: one LISTEN that hears every mode, a five-second link check before a five-minute send, help for radios without VOX (count-in, release cue, splitting into overs), text messages, and time on air shown everywhere.
6. **Video is too big for radio as it is.** 30 seconds of FlatRaster is about 12 kB, which with +50% repair is about 2.6 minutes on air at the fast speed, 8 at the robust one and 33 at the very robust one (estimate from the app's own burst timings). The useful radio products are a *postcard* (one small still and a voice message) and a *slideshow*. The biggest technical step is a small purpose-built coder for flat pictures instead of the phone's video encoder; the biggest legibility steps cost no bits at all (pick the sharpest picture, average camera frames, give the face its own colours, crop to the face, upscale smoothly at the receiver).

## 2. Decisions for the owner

Each has my recommendation first. The plan in section 9 assumes the recommendations.

**Decided 2026-11-15, when the owner asked to start:**

- **D2 to D10: the recommendations in the table are decided.** D6 still needs a name from the owner and blocks nothing.
- **D1 decided 2026-11-16: one repository**, in `apk-codec2-recorder`, as recommended in the table below. Stage 1 may move files. Per section 9.1 its first step (reference outputs in `tests/golden/`, the folder layout, the interfaces between layers) needs the stronger model; Sonnet does the moving and reformatting under those tests. Both package names and both signing keys stay.
- **Stage 0 built 2026-11-15** on Sonnet 5.5, in both repositories (2.8.1 and 0.15.1); waiting for the owner's run on a phone. Record: `PLAN.md` "Start here". The shared voice-speed code is copied into FlatRaster (`src/pl/dyktafon/SelfTest.java`, identical but public), so stage 1 must merge the two copies.

| # | Decision | Recommendation | Why |
|---|---|---|---|
| D1 | One repository or two | **Decided 2026-11-16: one**, in `apk-codec2-recorder`, with folders `core/`, `apps/recorder/`, `apps/flatraster/`. Both package names and both signing keys stay, so both apps install over their current versions | Ends the copying. A cloud session attaches one repository. FlatRaster's repository gets a note pointing here and is archived |
| D2 | Phone test first, or clean-up first | **Both at once.** The clean-up (stage 1) is made provably behaviour-identical by tests that compare output sample for sample, so it does not add risk to the untested releases | The phone test and the clean-up do not depend on each other |
| D3 | Default sound profile for DRED and RADE | **A new "Natural" profile** becomes the default once the owner has compared it by ear; today's chain stays as "Processed" | Section 5 |
| D4 | RNNoise (neural denoiser) | **Closed: not used.** The owner judged it not good in an earlier session (said 2026-11-15); the plan files had only recorded it as "blocked". The app's own denoiser stays, and is improved by measurement (section 5.4) | Owner's decision |
| D5 | Which phones to build for | **Drop the x86_64 library from the released APK** (only emulators use it; about a third of the 14 MB). Keep 32-bit ARM until the self-test shows its speed | Slimmer download |
| D6 | The app's name | Decide a name without "Codec2" in it (it records no Codec 2). Package `pl.dyktafon` stays | Already open as stage 10 |
| D7 | Old `.c2` handling | **Remove the special rows and the "open it in 2.6" messages** one version from now | The owner said old recordings don't matter; it is the most visible obsolete text |
| D8 | FlatRaster's own picture coder | **Measure it on a computer first** (stage 6a), build only if it clearly beats AV1 at these sizes | It is the largest single piece of new work |
| D9 | Neural video (person cut-out, pose "puppet") | **Not now.** Do the free steps first; then person cut-out as the first neural step; the puppet as an experiment on a computer | Section 7 |
| D10 | Call sign | Add a call sign setting, sent as plain text in every data transmission and optionally as Morse | Licence rules; also tells the receiver who is sending |

## 3. Review: what was found

### 3.1 Worth keeping as it is

- **No Gradle, no dependencies.** The whole build is one readable script. This is rare and valuable for a teaching project. Keep it.
- **Pure-Java core that runs on a computer.** `Fountain`, `RadioLink`, `Dred`, `Recorder`, `VoiceChain`, `Resampler`, `VoicePackets`, `AudioPlan` have no Android in them and are tested there.
- **Upstream sources untouched**, with flags in one file each, and the one needed patch applied to a copy (`radeflags.sh`).
- **Formats that fail safely.** `.dred` names its format and bitstream version and refuses what it cannot play; `.frv` packets check themselves; a cut-off file still plays.
- **Comments that say why**, with the measurement behind the number (`VoiceChain`, `flat.c`, `Fountain`).
- **Honest records.** The plans say plainly what was and was not tested.

### 3.2 Structure

| Finding | Where | Effect |
|---|---|---|
| Two activity classes do everything: page bridge, playback, WAV export, file compression, import, noise sampling, permissions, JSON | Recorder `MainActivity.java` (553 lines, 40 kB), FlatRaster `MainActivity.java` (491 lines) | Nothing can be tested or reused; every change touches the same file |
| Services and the page talk through global variables: 20 `static volatile` fields polled five times a second | `RecService.java:21-33`, `RadioService.java:24-28` | Hidden coupling; messages are lost if the page is not polling; hard to follow |
| The microphone loop is written five times | `RecService.java:109`, `RadioService.java:77` and `:147`, recorder `MainActivity.java:74`, FlatRaster `AudioCapture.java` and `NoiseStore.java` | A fix (Bluetooth routing, silent-microphone warning) must be made five times |
| The voice chain takes 11 unnamed arguments, and its defaults (0.18, 6, 12, 3, 180) are repeated | `VoiceChain` constructor; `RecService.java:53`, `RadioService.java:150`, `MainActivity.java:44`, `index.html:122`, FlatRaster `SoundSettings.java` | A default changed in one place silently differs in the others |
| Copy between repositories, already drifting | 7 identical files in `src/pl/dyktafon/`, `dredjni.c`, `third_party/opus-1.6.1`; FlatRaster added `SoundSettings`, `MicRoute`, `NoiseStore`, which the recorder still has inline | Two places to fix, and the better code is in the offshoot |
| Numbers kept twice, in Java and in the page | Burst seconds and frame sizes in `index.html:183` against `RadioLink.Sender.seconds()`; kB per minute in `index.html:94`; voice bit rates in FlatRaster's page | The page's time-on-air estimate can drift from what is really sent |
| JSON built by hand with string joins | Recorder `MainActivity.java:399-418` | A stray character in a name breaks the page; unreadable |
| Dense style | 45 lines over 180 characters in the recorder, several statements per line, one-letter names (`h`, `f`, `r`, `x`) | The main obstacle for a human reader |
| Tests are scattered, with no single command | Recorder: six commands in `README.md`. FlatRaster has `tests/run.sh` | Nobody runs them all |
| No description of the formats or the signal path outside the code | | A second implementation (a PC program, another app) cannot be written from the documents |

### 3.3 Defects and leftovers

None of these is urgent; all are small. They are listed so the clean-up removes them.

| Finding | Where |
|---|---|
| The noise sample taken while idle goes through a different high-pass filter (a simple DC blocker) than the one used when recording (180 Hz, second order). The saved noise picture therefore holds more low-frequency noise than the recording path ever sees | Recorder `MainActivity.java:79`, FlatRaster `NoiseStore.java:34` |
| The denoiser computes a speech score for voice activation and nothing uses it; voice activation uses band level only. The class comment says otherwise | `VoiceChain.java:59`, `Denoiser.java:46` |
| LISTEN keeps all received speech in memory until the message ends. A stuck carrier or false lock grows it by 115 MB an hour | `RadioService.java:233` |
| `bash build.sh test` still labels the build "2.7-test1" | `build.sh:35` |
| Live-voice leftovers after 2.8: `talk` flag, `#ptt` and `#lctl` styles, voice functions of the modem layer (`speechN`, `modemN`, `txVoice`, `rxVoice`, mode 11) | `RadioService.java:24`, `index.html:31-32`, `Radio.java`, `fdvjni.c` |
| `tests/ui.py` needs Python Playwright, which sessions do not have, and its stand-in app still lists a Codec 2 recording. `tests/ui_radio.js` has replaced it | `tests/ui.py` |
| Three picture buffers allocated and never used | FlatRaster `flat.c:10` ("unused since 0.7") |
| FlatRaster stops recording whenever the app leaves the screen. Right for the camera, but it is nowhere stated to the user | FlatRaster `MainActivity.java:161` |
| The repair-frame solver redoes the whole elimination for every frame that arrives after the first `k`. Fine at today's sizes; at the 100 kB limit in the slow modes it would be seconds per frame | `Fountain.java:104` |
| One wrong frame that passes the modem's check makes the receiver throw away everything received | `Fountain.java:148` |
| The build hides compiler warnings and the first errors behind `grep ... | head -8` | `build.sh:22-25` |

### 3.4 Usability for the real use

The use: no grid, a phone pressed against the microphone of a handheld, CB or HF radio, to pass voice, small pictures, text and files, or to store them in very little space.

- **Both sides must choose the same speed by voice before sending.** The receiver could listen for all three speeds at once (three receivers on the same sound) and for RADE too.
- **Nothing tells the sender whether the link works before a long send.** A 5-second probe that the other phone answers with its signal reading would.
- **The send assumes VOX.** Most handhelds and CB sets are keyed by hand, and many handhelds cut a transmission after 1 to 3 minutes (time-out timer). The app sends 3 to 8 minutes in one go with a 1-second lead-in.
- **The smallest useful payload is missing.** A typed text of 200 bytes passes in one burst at the fast and robust speeds (17 and 13 seconds) and in two at the very robust one (42 seconds); these are estimates from the burst timings. Today text must be made in another app and sent as a file.
- **A photo cannot be sent**: any photo is over the 100 kB limit, and the app offers no way to shrink it.
- **The radio's own sound path is not in the tests.** `tests/radio/chan.py` models noise, fading, room echo and a distorted speaker, but not an SSB filter (300 to 2700 Hz) with AGC, nor an FM handheld (pre-emphasis, deviation limiter, de-emphasis). For "phone at a handheld" this is the channel.
- **The pages explain with paragraphs** (RADE tab, Files tab, the level bar) instead of with layout, and name the technology (DRED, RADE, DATAC) where the user wants the task.
- **FlatRaster shows 30 settings in one grid**, video and sound mixed, with no hint of what each costs in bytes.

## 4. Target structure

One repository, three layers, each allowed to know only the one below it.

```
core/        Pure Java and C. No Android. Runs and is tested on a computer.
  audio/     the voice chain, one small class per stage; resampler; meters
  codec/     Dred (file format), DredCoder, DredPlayer, Rade; one table: bitstream version -> library
  link/      Fountain (repair frames), Modem (FreeDV), RadioLink, Message (what a transmission holds), Probe
  picture/   posterize filter, picture coder, .frv format        (FlatRaster only)
  settings/  one description of every setting: name, choices, default, help text
  native/    dredjni.c, radejni.c, fdvjni.c, flat.c
android/     The only code that touches Android.
  MicInput, SpeakerOutput   (the one microphone loop, the one player)
  RecordingService, RadioService, CameraSource
  Storage, Share, Permissions
  Bridge     page <-> app, one JSON message type each way
ui/          index.html, style.css, and one script per tab
apps/recorder/     manifest, icon, list of tabs
apps/flatraster/   manifest, icon, list of tabs (adds the picture tab and the camera permission)
third_party/ unchanged upstream sources
tests/       run.sh runs everything; golden/ holds reference outputs
docs/        the documents below
tools/       the same core as a command-line program for a PC
```

Rules of the house, written into `docs/style.md`:

1. **One statement per line, lines up to 120 characters, names that are words.** Units in the name where it helps (`holdMs`, `cornerHz`).
2. **One stage, one class, one comment block**: what it does, why it is there, the formula, the measured effect, and where the numbers come from. The voice chain becomes a list of stages that reads top to bottom like the block diagram.
3. **Settings travel as one object** (`SoundSettings`, already in FlatRaster), built from the one settings description. No positional arguments, no repeated defaults.
4. **State flows one way.** A service publishes one status object; the page receives it as JSON made by a small writer in `core`. No global variables.
5. **Every number shown to the user is computed by the core**, never copied into the page.
6. **A format is a document first.** `docs/format-dred.md`, `docs/format-frv.md`, `docs/link.md` (frame layout, repair frames, bursts) are the truth; the code cites them. Amateur rules also expect a publicly described format.
7. **A new coder goes in beside the old one.** Each library is named by what it decodes (`libdred12.so`), each file names its bitstream, and one table maps one to the other. This is already the rule for Opus; the structure makes it mechanical.
8. **Nothing changes sound or picture without a number.** A refactor must leave `tests/golden/` byte-identical. A change of behaviour must come with its measurement in the commit.

Documents to write (short, each one or two pages): `architecture.md` with the layer drawing, `signal-chain.md` with the block diagram and what each stage costs and gains, `format-*.md`, `link.md`, `measuring.md` (how to repeat every table in the plans), `build.md`, `style.md`.

The command-line program in `tools/` is nearly free, because the tests already build the native libraries and the core for a computer. It gives: a receiving station that is a laptop, a second implementation that proves the format documents, and faster tests.

## 5. Sound: what to change for clarity

### 5.1 What the measurements already say

From `PLAN.md`, stage 9 part B (8 English speakers, DRED small, intelligibility estimate STOI, higher is better):

| Path | Clean speech |
|---|---|
| DRED alone | 0.81 |
| Whole recording chain, defaults | 0.72 |
| Chain with treble, lift, compressor, gate off | 0.76 |
| The same with fixed gain instead of automatic level | 0.81 |

So the default chain costs as much clarity as going from "small" to "smallest" quality, and the automatic level is the largest part. The same loss shows in FlatRaster (0.77 to 0.63, `PLAN-dred.md`). The plan rightly warns that STOI also counts intended changes as loss. Still, the direction is clear and the reason is known: DRED and RADE were trained on natural speech. Treble boost, consonant lift, a compressor and a level that follows each syllable all move the input away from what the coder learned.

### 5.2 The rule

- **Before the coder: only repairs.** Remove rumble, remove noise, set the level slowly, catch peaks. Everything the coder would waste bytes on or be confused by.
- **After the decoder: everything for the listener.** Treble, consonant lift, compression, loudness, speed. It costs no bytes, cannot damage the recording, and can be set for the moment of listening (earphones, a loud room, a tired ear).

### 5.3 The "Natural" profile to measure (stage 3)

| Stage | Today | Natural profile | Reason |
|---|---|---|---|
| Rumble cut | 180 Hz, second order | 80 to 100 Hz | At 16 kHz the coder models the voice's pitch; a 180 Hz cut weakens the fundamental of most male voices |
| Denoise | Wiener filter, medium | Unchanged | It never hurt and helped a little in noise (`PLAN.md` part B). RNNoise is ruled out (D4) |
| Level | Follows peaks, falls 40% per 40 ms frame | Slow: measure speech loudness over 2 to 3 s while voice is present, move at most 3 dB per second, hold still in pauses | The measured main loss |
| Gate | -12 dB in pauses | Off when Denoise is on | Two stages doing one job |
| Treble, consonant lift, compressor | On | Off before the coder; offered at playback | The rule above |
| Limiter | Ceiling 18000 | Unchanged | Measured, and still needed |

### 5.4 More candidates, each an experiment that stays only if it measures better

1. **Spectrum tilt correction.** A slow equaliser pulls the long-term spectrum of the voice toward that of average speech. Corrects a muffled or far microphone, a phone in a pocket, a mask. Cheap: the denoiser already works per frequency band.
2. **Capture at 48 kHz and convert down in the app** (the resampler exists). Some phones process sound differently at 16 kHz; 48 kHz is the path they treat best. Needs the phone to compare.
3. **Wind guard.** When bursts of low-frequency energy appear, raise the rumble corner for as long as they last.
4. **Echo of the room** (speaker far from the phone): suppress the late tail in the same band-gain stage as the denoiser.
5. **Two microphones.** Most phones have two; recording both lets the app keep the one with the better voice-to-noise ratio, or combine them. Phone-dependent; later.
6. **Quality that varies per second.** A DRED block carries its own quality step, so the file format need not change. Two uses: finer steps where speech is dense, and *fit to a budget* ("make this message at most 40 seconds on air").
7. **Use the speech score** the denoiser already computes for voice activation (finding in 3.3), and compare it with the present level rule on quiet speech.

Playback side, all cheap:

- Treble, compressor and consonant lift as listening options.
- **Speed control with pitch kept** (Android's player does it; no code size). Slowing unclear speech a little often helps more than any filter.
- A "back 5 seconds" button.

### 5.5 Measuring it properly

The present rig (`tests/dred/run.py`, `chain.py`) is good. It lacks three things:

- **Real Polish speech and real noise.** No Polish recordings could be fetched in earlier sessions. Add a "save test clip" switch to the diagnostic screen (stage 0) that stores 30 s of raw microphone sound; the owner records Polish in a quiet room, a car, wind, a street. Those clips become the test set.
- **A word count beside STOI.** Run an offline recogniser (Whisper or Vosk, on the computer only) on the decoded sound and count wrong words, for Polish and English. STOI cannot tell a pleasant change from a harmful one; a word count can.
- **The owner's ears.** A listening page as in stage 9: same sentence, "Processed" against "Natural", clean and noisy. The decision D3 is made there.

## 6. Radio link

### 6.1 Make the link usable by hand (stage 5)

1. **One LISTEN for everything.** Run the three data receivers and the RADE receiver on the same microphone sound. Whatever arrives lands in one inbox. Removes "both sides need the same speed".
2. **Link check.** SEND TEST sends one very robust burst holding the call sign and a counter. A phone that hears it shows signal-to-noise ratio and suggests a speed; with a radio at both ends it can answer with one burst carrying that reading.
3. **Manual keying.** A setting "My radio: VOX / I press the button". With the button: the screen says PRESS, counts down, sends, then vibrates and says RELEASE.
4. **Overs.** A setting "longest transmission" (default 60 s). A long send is cut into overs with a pause between them. Repair frames already make every burst independent, so a missed over costs nothing but time. This also respects time-out timers and lets others break in.
5. **Call sign** in the header of every data transmission, shown at the receiver; optional Morse identification at the end (a tone generator, a few lines).
6. **Ask for more with one number.** Because any repair frame helps equally, a receiver that is short does not need to list what is missing. It sends "need 7 more"; the sender sends 7 new repair frames. This replaces the planned missing-frame list of stage 5 in `PLAN.md` and is much simpler.
7. **Small gains already noted in `PLAN.md`**: a shorter last burst, and a shorter frame header (6 of 54 bytes at the very robust speed).

### 6.2 What to send (stage 5)

| Payload | Size | Notes |
|---|---|---|
| Text message | 50 to 500 bytes | Typed in the app. Compressed with the built-in deflate and a word list for Polish and English (helps most on short texts). One or two bursts at any speed |
| Position | about 20 bytes | Latitude, longitude, time; shown as numbers and locator. Needs the location permission, asked only when used |
| Picture | 1 to 5 kB | "Fit to N seconds on air": shrink and re-code with Android's built-in WebP coder, or as a flat picture or sketch (section 7) |
| Recording | 2 to 10 kB per minute | As today |
| Any file | up to 100 kB | As today |

Everything the user picks shows its time on air at the current speed before SEND.

### 6.3 Test what the radio does to the sound (stage 5, on a computer)

Add to `tests/radio/chan.py`:

- **SSB voice channel**: band-pass 300 to 2700 Hz, receiver AGC that pumps up in the gaps between bursts, transmit ALC.
- **FM handheld**: pre-emphasis, deviation limiter (hard clipping), de-emphasis, band-pass 300 to 3000 Hz, a squelch that opens late.
- **CB AM**: band-pass, envelope clipping.

Run all three data speeds and RADE V1 through them. This tells, before any radio is touched, which speeds survive a handheld and at what speaker level. If the fast speed survives FM easily, look for a faster one: the modem source holds a 16-QAM setting ("qam16" in `ofdm_mode.c`) that the public interface does not offer. Whether it can be reached without editing upstream files is not checked.

### 6.4 Later, as in `PLAN.md`

Cable and USB audio with keying (stage 4 there), real-time RADE talk, RADE V2 when FreeDV releases it. Unchanged. A cable remains far more reliable than sound through the air; `docs/` should show how to make one.

Note on rules: amateur bands need the call sign in plain form and a publicly described format (both covered above). CB and PMR446 rules in most countries allow voice only; sending data tones there is the operator's responsibility.

## 7. Picture: FlatRaster

### 7.1 Where the bytes go

Measured on synthetic clips (`PLAN.md` of FlatRaster): video 2 to 3.5 kbit/s at 320x240 and 3 pictures per second, with AV1 from a computer encoder. Voice at the default level is about 0.6 kbit/s. So the picture is 80% of the file. On a real phone it is probably worse: few phones have an AV1 encoder, and hardware HEVC and H.264 encoders usually refuse bit rates this low. This is not measured; the self-test (stage 0) will report which encoder the phone gave and what it produced.

### 7.2 Free legibility: no extra bytes (stage 6b)

| Step | What it does | Cost |
|---|---|---|
| **Pick the sharpest picture** | The camera gives 15 to 30 pictures a second and the app uses 0.5 to 5. Of those in each interval, keep the sharpest (edge energy), not the one that happens to be due | A few lines |
| **Average the camera's pictures** | Where nothing moves, average the pictures between two stored ones. Noise falls, borders steady, and the stream shrinks | A few lines in `flat.c` |
| **Face first in the palette** | Count pixels inside the face box several times when choosing colours. Today a wall can take six of eight colours and the face two | A few lines |
| **Local contrast in the face box** | Stretch brightness inside the box before posterizing, so eyes, brows and mouth land on different colours in poor light | Small |
| **Smooth upscaling in the player** | Enlarge the flat picture with a rule that follows the colour borders, not with blocks or blur. A 160x120 picture then looks like a clean drawing on a full screen | Player only; old files gain too |
| **Grey palette option** | Four or six fixed greys: no palette to send, no palette changes, and faces read well in grey | Trivial |

### 7.3 Fewer bytes without neural networks (stage 6b, 6c)

1. **Crop to the face** (already step 1 of FlatRaster's plan). The camera's own face detector gives the box. Head and shoulders at 96x128 is a sixth of the pixels of 320x240. Move the crop rarely and in steps.
2. **Hold the eyes still.** Where the camera reports eye and mouth points (its "full" face mode; not every phone), keep the eyes at fixed places in the crop. A head that sways no longer changes the picture, so the differences between pictures shrink and the face is easier to read.
3. **Sketch mode.** A one-bit line drawing (edges found by the difference of two blurs) instead of colours. Faces stay recognisable, and it is the right mode for **documents**: a photo of a handwritten note or a hand-drawn map becomes a few hundred bytes of crisp black on white.
4. **Still background.** With the phone standing still, which is the normal case beside a radio, send the background once and afterwards only the region that moves, pasted in by the player. The repeating-movement filter already keeps the per-pixel history this needs. Hand-held needs shake compensation first.

### 7.4 A coder made for flat pictures (stage 6a measures, 6c builds)

A posterized picture is a map of colour numbers with large flat areas. A video encoder is the wrong tool for it: it was designed for natural pictures and works on brightness blocks. A purpose-built coder does this instead:

- **Full picture**: the palette, then each pixel's colour number coded with an arithmetic coder that predicts it from the neighbours already sent (left, above, above-left, above-right). Inside a flat area a pixel costs almost nothing; only border pixels cost bits.
- **Difference picture**: which pixels changed (a one-bit map, coded the same way), then the new colour numbers for those.

Estimate, not measured: a full picture of 160x120 in 8 colours in roughly 0.3 to 0.8 kB, and of 320x240 in roughly 0.8 to 1.5 kB, against several kB for a video key picture; a difference picture of a still scene in tens of bytes. About 300 lines of C with no dependencies, the same result on every phone, readable as a teaching piece, and decodable on a PC. It also removes the need to bundle a software AV1 encoder (several MB), which FlatRaster's plan lists for phones without one.

MP4 export stays: the app draws the decoded pictures and gives them to the phone's encoder, as now.

Stage 6a is the test: take the existing posterized test clips, code them with a prototype in Python, and compare with the AV1 figures already in the plan. Decide then (D8).

### 7.5 Neural networks: an honest ranking

| Option | Bytes | Added to the app (to verify) | What is gained | What is lost | Verdict |
|---|---|---|---|---|---|
| Face box from the camera (7.3) | Saves about 5/6 | Nothing | Bits go to the face | The surroundings | **Do first** |
| Eye and mouth points from the camera (7.3) | Saves more | Nothing | A steady face | Works on some phones only | **Do second** |
| Person cut-out (segmentation model) | Background sent once or flattened | Model under 1 MB, runtime library 1 to 4 MB per processor type | Works hand-held and when the person turns away | Size; invents nothing | **First neural step**, if any |
| Body pose, the "puppet" (17 to 33 points) | 30 to 70 bytes per picture | Model 3 to 9 MB, the same runtime | Gestures and movement at a few hundred bit/s | Who it is, what they hold, their face | **Experiment on a computer** before any phone work. Useful for signing or watching movement, not for seeing a person |
| Face mesh (hundreds of points) drawn as a mask | 100 to 300 bytes per picture | As above | Lip and brow movement | The face itself | Not recommended: a still picture and a voice tell more |
| Face re-animation from one photo | Lowest of all | Tens of MB, heavy for a phone | A moving face | Truth: it draws details that were not there | **No.** In an emergency tool an invented face is worse than none |

The runtime library must be linked by hand into this Gradle-free build (NCNN or the TensorFlow Lite C library are the candidates). Whether that works, and the sizes above, are from memory and must be checked first.

### 7.6 Products for radio (stage 6d)

- **Postcard**: one picture (face crop or sketch, 0.5 to 2 kB) and up to 20 s of voice at the smallest level (about 0.7 kB). Around 2 kB: about 35 s on air at the fast speed, about a minute and a half at the robust one (estimate, with +50% repair). This is the feature that fits the use: "this is me, I am well, I am here".
- **Slideshow**: a voice recording with a picture whenever the scene changes enough or the user taps. A minute of it is 3 to 6 kB.
- **Document**: a sketch-mode photo of paper.
- Video proper stays for storage and for good links.

## 8. Interface

### 8.1 Principles

1. **Tasks, not technologies.** Tabs: **Record · Send · Receive**, and a settings screen. FlatRaster adds **Picture**.
   - *Send*: choose what (voice message, recording, text, picture, file), see the time on air, press SEND. "Voice message" is RADE; the page says "heard by FreeDV stations" and keeps the name RADE in the help.
   - *Receive*: one LISTEN, a signal bar, a small waterfall, the inbox.
2. **No paragraphs on the working screen.** Each tab gets a "?" that opens its help; the help is shown once on first use. The three long explanations (level bar, RADE, Files) move there.
3. **Presets first, stages behind them.** Sound: *Natural / Noisy place / Far microphone / Custom*. Picture: *Radio (smallest) / Balanced / Detail / Custom*. "Custom" opens the stages **in signal order**, each with an on-off switch and a small meter, so the panel is the block diagram of `docs/signal-chain.md`. This is also what makes the app teach.
4. **Hear before you commit.** A TRY button: record 5 seconds, play it back through the coder with the present settings. The RADE tab already has this idea (HEAR AS RADE).
5. **Every choice shows its price** in kB per minute and in time on air at the chosen speed, computed by the core.
6. **One description of the settings** (`core/settings/`): name, choices, default, help text. The page is drawn from it and the app reads it, in both apps. The duplicated defaults disappear.
7. **A hand that is busy.** While sending: one large STOP, a countdown, vibration at the start and end. The phone is being held against a radio; nobody is reading small print.

### 8.2 To remove

- The `.c2` rows, their messages and the fallback for stored Codec 2 modes (D7).
- "Sound, as in Codec2 Recorder" as a heading in FlatRaster; the words "recommended" inside choices (the default is the recommendation).
- "DRED" in front of every quality name: *Best, Balanced, Small, Smallest* with the size is enough; the coder's name goes in the help and the file details.
- Gain in dB and the routed microphone from the main status line; they move to a details line under SETTINGS.
- Dead styles and controls from the removed live-voice tab (3.3).
- In FlatRaster, settings that measurement has already decided: "Show as the previous colour" and "New colours: every picture" cost 5 to 32% more bytes and did not improve borders (FlatRaster `PLAN.md`); keep them only in Custom, or remove them after the phone comparison.

## 9. The stages, in order

Each stage ends with something that can be checked. "Computer" means no phone is needed.

| # | Stage | Done when | Needs | Model |
|---|---|---|---|---|
| 0 | **Self-test and diagnostics** in both apps: speed of DRED and RADE coding and playing on this phone; modem loopback through the phone's own speaker and microphone at three volumes (frames received, signal-to-noise ratio, clipping); which video encoder the phone gives and its real bit rate; "save test clip" (30 s raw sound); "save what the microphone heard". One button, one text report to share | The owner runs it once and pastes the report. It answers most of the 2.7 and 2.8 phone lists without a second phone or a radio. **State 2026-11-15: built and tested on a computer, Codec2 Recorder 2.8.1 and FlatRaster 0.15.1; waiting for the owner's run on a phone** (what it covers and leaves out: `PLAN.md` "Start here") | Computer to build, phone to run | Sonnet |
| 1 | **One repository, shared core, readable code** (D1). First record reference outputs in `tests/golden/` from today's code; then move and reformat; then split the activities, replace the global variables, unify the microphone loop and the settings object. Fix the list in 3.3. `tests/run.sh` runs everything; a GitHub workflow runs it on every push | Both apps build from one tree; reference outputs are byte-identical; no line over 120 characters; versions 2.9 and 0.16 behave exactly as 2.8 and 0.15 | Computer | Stronger model for the first step (reference outputs, the folder layout, the interfaces between layers); Sonnet for moving, reformatting and splitting under those tests |
| 2 | **Documents** (section 4) and the PC command-line program | A person can write a `.dred` reader from `docs/format-dred.md` alone; `tools/` receives a file from a WAV made by the app's tests | Computer | Sonnet |
| 3 | **Sound**: the measuring rig with word counts and the owner's clips; the Natural profile; listener-side processing and playback speed; candidates of 5.4 one by one | A table: every profile and candidate, clean and noisy, Polish and English, STOI and wrong words. The owner's listening page. D3 decided and the confirmed settings written into `CLAUDE.md` | Computer, then the owner's ears | Stronger model |
| 4 | **Interface**: Record, Send, Receive; presets and stages in signal order; help sheets; TRY; prices; obsolete text removed | The page tests pass against the new bridge; the owner can do each task without reading a paragraph | Computer, then phone | Sonnet |
| 5 | **Radio by hand**: LISTEN for all modes, link check, manual keying and overs, call sign, text, position, picture to fit, "need N more"; SSB, FM and CB channel models | Every payload passes the new channel models at the speeds the tables name; a text passes at the very robust speed in under a minute | Computer, then two phones, then radios | Stronger model for the channel models, LISTEN for all modes and "need N more"; Sonnet for text, call sign, keying, overs and the page |
| 6a | **Picture coder measurement** | A table: prototype coder against AV1 on the existing clips. D8 decided | Computer | Stronger model |
| 6b | **Free legibility** (7.2) and face crop, eye hold, sketch (7.3) | Each step has a before-and-after figure for bytes and for border quality, as FlatRaster's tests already produce | Computer, then phone | Stronger model |
| 6c | **The coder in the app**, still background, `.frv` version 3 with short packet headers (old files still play) | A 30 s recording is under half its present size at equal legibility | Computer, then phone | Stronger model |
| 6d | **Postcard, slideshow, document** over the radio link from the shared core | A postcard passes phone to phone at the robust speed in under 2 minutes | Two phones | Sonnet |
| 7 | **Options**, each on its own decision: transcription to text (try Android's own offline recogniser first, it adds nothing to the app; whisper.cpp second); person cut-out; cable and keying; real-time RADE; RADE V2; two-way link | | | Named when each option is decided |

### 9.1 Which model does which stage

"Sonnet" is the cheaper, faster model; "stronger model" means the most capable one available (Opus class or above). The split follows one question: can a test catch a mistake?

- **Sonnet** where the work is spelled out and a test guards it: moving and reformatting code under `tests/golden/`, the page and its bridge, documents, the self-test screen, wiring features to the core.
- **Stronger model** where a wrong but plausible result would be written into the plan and built on: sound and picture measurements, the picture coder, channel models, the design of interfaces between layers, and faults in the native layers.

How the switch is made, either way:

1. The owner picks the model when starting the session (`/model`, or the model picker in the app). Each session says at its start which model it runs on and whether that fits the stage it is about to do.
2. A session on the stronger model hands a Sonnet stage to a Sonnet helper (the session can start one itself), then checks the result: `tests/run.sh` must pass, reference outputs included, before anything is committed.

Rules for a Sonnet session or helper:

- It may not change a sound or picture default, a file format, or a number in the plan's tables.
- It stops and writes the problem into `PLAN.md` "Start here", for a stronger-model session, when: a reference output differs and the cause is not obvious; a native library crashes or gives different output; a measurement disagrees with a table in the plans.
- Its work is accepted only with the whole test run passing.

Stage 0 is built in Codec2 Recorder first and needs no decision; FlatRaster gets the same self-test when its code joins the shared core in stage 1 (or by copying, if D1 turns out to be two repositories). What can run side by side: stages 1 and 2 need only D1. Stage 3 and stage 6a are independent measurements. Stages 4 and 5 should follow stage 1, because they rewrite the page and the bridge.

How this meets the existing plans: the phone test lists at the top of `PLAN.md` stay valid, and stage 0 here covers most of them. Stages 4 to 6 of the recorder's `PLAN.md` (cable, two-way link, FlatRaster over radio) become stages 7, 5 and 6d here. `PLAN-radio.md` in FlatRaster is superseded by section 6 and stage 6d: the transport it plans to copy comes from the shared core.

## 10. Features worth adding, ranked by use per effort

| Feature | Why | Effort |
|---|---|---|
| Self-test with a shareable report | Unblocks everything | Small |
| Text messages | The smallest payload, gets through when nothing else does | Small |
| One LISTEN for every mode, with an inbox | Removes the main operating mistake | Medium |
| Link check | Saves long failed sends | Small |
| Manual keying, overs, vibration cues | Makes handhelds and CB sets usable | Small |
| Playback speed and "back 5 s" | Helps understanding at no cost | Small |
| Postcard (picture and voice) | The reason to have a camera in a radio tool | Medium, after 6b |
| Document and sketch mode | Notes and maps in a few hundred bytes | Medium |
| Waterfall on Receive | Tuning an SSB radio by eye; also teaches | Small |
| Fit to a time on air | The user thinks in seconds, not kB | Medium |
| Bulletin carousel: a list of files repeated in turn | A station that broadcasts news or lists; late listeners still complete, thanks to repair frames | Small |
| Pass it on: SEND on anything received | Relaying across stations | Small |
| Received text read aloud (Android's own speech) | Hands and eyes busy | Small |
| Voice to text before sending | A minute of speech as 200 bytes instead of 2 kB | Large; stage 7 |
| PC program | A laptop as the other station; proves the formats | Small after stage 1 |
| Call sign and Morse identification | Rules | Small |

## 11. What not to do

- **Do not add Gradle, a UI framework or a dependency manager.** The hand build is the project's character and its teaching value.
- **Do not unify `.dred` and `.frv` into one format now.** Every format is a reader to keep for ever. Document both; change `.frv` once, with the picture coder.
- **Do not change a sound or picture default without its measurement** in the same commit. The existing rule in `CLAUDE.md` is right.
- **Do not ship anything that invents content** (face re-animation, neural upscaling). The apps' honesty about "clean but wrong" speech should extend to pictures.
- **Do not build more features on the untested releases** before stage 0 has reported from a phone.
