#!/usr/bin/env python3
"""DRED recording experiment (PLAN.md stage 9, part A).

  python3 tests/dred/run.py SPEECH_DIR OUT_DIR [VOSK_MODEL_DIR]

SPEECH_DIR holds 16 kHz, 16 bit, mono .raw files. Needs tests/dred/dredtest and tests/dred/c2rt
(tests/dred/build.sh), numpy, scipy and pystoi.

For every file it also makes a noisy copy (pink noise, 10 dB below the speech), then codes all of them
with DRED at several quality levels and with Codec 2, and prints size and STOI. STOI is a standard
0 to 1 estimate of how much of the speech can be understood, measured against the clean original.
It is not a listening test. OUT_DIR gets WAV files for listening.

With VOSK_MODEL_DIR (an English Vosk model, and the vosk Python package) it also counts the words an
offline recogniser gets wrong on files whose names start with en_.
"""
import os, subprocess, sys, time, wave
import numpy as np
from scipy.signal import resample_poly, stft
from pystoi import stoi

HERE = os.path.dirname(os.path.abspath(__file__))
DRED, C2 = os.path.join(HERE, "dredtest"), os.path.join(HERE, "c2rt")
FS = 16000
QS = [4, 7, 9, 12, 15]
C2MODES = ["3200", "1300", "700C", "450"]
EVERY = 50  # one DRED block per second


def run(*a):
    return subprocess.run(a, check=True, capture_output=True, text=True).stdout


def load(p):
    return np.fromfile(p, dtype=np.int16).astype(np.float64)


def save(p, x):
    np.clip(np.round(x), -32768, 32767).astype(np.int16).tofile(p)


def wav(p, x, fs=FS):
    with wave.open(p, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(fs)
        w.writeframes(np.clip(np.round(x), -32768, 32767).astype(np.int16).tobytes())


def pink(n, rng):
    f = np.fft.rfftfreq(n, 1 / FS); f[0] = f[1]
    return np.fft.irfft(np.fft.rfft(rng.standard_normal(n)) / np.sqrt(f), n)


def env(x, hop=16):
    n = len(x) // hop
    return np.log(np.sqrt((x[:n * hop].reshape(n, hop) ** 2).mean(1)) + 10)


def align(ref, out):
    """Delay of out against ref in samples, from the loudness contour, to 1 ms."""
    a, b = env(ref), env(out)
    best, lag = -2, 0
    for d in range(-40, 160):
        if d >= 0: x, y = a[:len(a) - d], b[d:]
        else: x, y = a[-d:], b[:len(b) + d]
        n = min(len(x), len(y)) - 100
        c = np.corrcoef(x[50:50 + n], y[50:50 + n])[0, 1]
        if c > best: best, lag = c, d
    return lag * 16


def shifted(out, lag, n):
    y = out[lag:] if lag >= 0 else np.concatenate([np.zeros(-lag), out])
    return np.concatenate([y, np.zeros(max(0, n - len(y)))])[:n]


def score(clean, out):
    lag = align(clean, out)
    return stoi(clean, shifted(out, lag, len(clean)), FS, extended=False), lag


def dred(src, tmp, q, every=EVERY):
    info = run(DRED, "enc2", str(q), str(every), src, tmp + ".dred")
    run(DRED, "dec2", str(every), tmp + ".dred", tmp + ".raw")
    return float(info.split("kB_per_min")[1]), load(tmp + ".raw")


def codec2(src8, tmp, mode):
    info = run(C2, mode, src8, tmp + ".raw")
    return float(info.split("kB_per_min")[1]), resample_poly(load(tmp + ".raw"), 2, 1)


def bands(x):
    _, _, z = stft(x, FS, nperseg=512, noverlap=352)  # 10 ms hop
    p = np.abs(z) ** 2
    edges = np.unique(np.geomspace(4, 250, 21).astype(int))
    return np.array([10 * np.log10(p[edges[i]:edges[i + 1]].mean(0) + 1) for i in range(len(edges) - 1)])


def joins(clean, out, lag, every):
    """Mean spectral error (dB) in the 40 ms around block joins, and everywhere else."""
    y = shifted(out, lag, len(clean))
    d = np.abs(bands(clean) - bands(y)).mean(0)
    hop = 160; step = every * 320
    at = np.zeros(len(d), bool)
    for s in range(step, len(clean), step):
        c = s // hop
        at[max(0, c - 2):c + 3] = True
    at &= np.arange(len(d)) > 100  # skip the start
    rest = ~at & (np.arange(len(d)) > 100)
    return d[at].mean(), d[rest].mean()


def edits(a, b):
    """Word-level edit distance."""
    d = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        prev, d[0] = d[0], i
        for j, y in enumerate(b, 1):
            prev, d[j] = d[j], min(d[j] + 1, d[j - 1] + 1, prev + (x != y))
    return d[len(b)]


def words(model_dir, heard, coders):
    """English files only (names starting en_): what an offline recogniser makes of each version,
    counted against its own transcript of the clean uncoded file. Small Vosk English model."""
    import json
    from vosk import Model, KaldiRecognizer, SetLogLevel
    SetLogLevel(-1); model = Model(model_dir)

    def text(x):
        r = KaldiRecognizer(model, FS)
        r.AcceptWaveform(np.clip(np.round(x), -32768, 32767).astype(np.int16).tobytes())
        return json.loads(r.FinalResult())["text"].split()

    names = sorted({n for n, _ in heard if n.startswith("en_")})
    ref = {n: text(heard[(n, "clean")]["Uncoded"]) for n in names}
    total = sum(len(r) for r in ref.values())
    print(f"\n### Words a recogniser gets wrong, English files, {total} words\n")
    print("| Version | Clean input | Noisy input |\n|---|---|---|")
    for c in coders:
        cells = []
        for tag in ("clean", "noisy"):
            wrong = sum(edits(ref[n], text(heard[(n, tag)][c])) for n in names)
            cells.append(f"{100 * wrong / total:.0f}%")
        print(f"| {c} | {cells[0]} | {cells[1]} |")


def main():
    src_dir, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    rng = np.random.default_rng(1)
    names = sorted(f[:-4] for f in os.listdir(src_dir) if f.endswith(".raw"))
    sizes = {}; rows = []; join_rows = []; heard = {}
    for name in names:
        clean = load(os.path.join(src_dir, name + ".raw"))
        noise = pink(len(clean), rng)
        noisy = clean + noise * np.sqrt((clean ** 2).mean() / (noise ** 2).mean()) * 10 ** (-10 / 20)
        for tag, x in (("clean", clean), ("noisy", noisy)):
            base = os.path.join(out_dir, f"{name}_{tag}")
            save(base + "_in.raw", x); wav(base + "_in.wav", x)
            x8 = resample_poly(x, 1, 2); save(base + "_in8.raw", x8)
            row = {"file": name, "kind": tag, "uncoded": stoi(clean, x, FS, extended=False)}
            h = heard.setdefault((name, tag), {"Uncoded": x, "Uncoded, cut to 4 kHz": resample_poly(x8, 2, 1)})
            for q in QS:
                kb, y = dred(base + "_in.raw", f"{base}_dred_q{q}", q)
                s, lag = score(clean, y)
                row[f"DRED q{q}"] = s; sizes.setdefault((f"DRED q{q}", tag), []).append(kb)
                wav(f"{base}_dred_q{q}.wav", y); h[f"DRED q{q}"] = y
                if q == 7 and tag == "clean":
                    j, r = joins(clean, y, lag, EVERY)
                    kb10, y10 = dred(base + "_in.raw", f"{base}_dred_q7_every10", 7, 10)
                    s10, _ = score(clean, y10)
                    join_rows.append((name, j, r, s, s10, kb, kb10))
            for m in C2MODES:
                kb, y = codec2(base + "_in8.raw", f"{base}_c2_{m}", m)
                row[f"Codec 2 {m}"], _ = score(clean, y); sizes.setdefault((f"Codec 2 {m}", tag), []).append(kb)
                wav(f"{base}_c2_{m}.wav", y); h[f"Codec 2 {m}"] = y
            rows.append(row)
    coders = [f"DRED q{q}" for q in QS] + [f"Codec 2 {m}" for m in C2MODES]

    print("### Size, kB per minute (range over the files)\n\n| Coder | Clean input | Noisy input |\n|---|---|---|")
    for c in coders:
        a, b = sizes[(c, "clean")], sizes[(c, "noisy")]
        print(f"| {c} | {np.mean(a):.1f} ({min(a):.1f} to {max(a):.1f}) | {np.mean(b):.1f} ({min(b):.1f} to {max(b):.1f}) |")

    print("\n### STOI against the clean original (1.00 = all understood)\n")
    print("| File | Input | Uncoded | " + " | ".join(coders) + " |\n|---|---|---|" + "---|" * len(coders))
    for r in rows:
        print(f"| {r['file']} | {r['kind']} | {r['uncoded']:.2f} | " + " | ".join(f"{r[c]:.2f}" for c in coders) + " |")
    for tag in ("clean", "noisy"):
        sel = [r for r in rows if r["kind"] == tag]
        print(f"| **mean** | {tag} | {np.mean([r['uncoded'] for r in sel]):.2f} | " + " | ".join(f"{np.mean([r[c] for r in sel]):.2f}" for c in coders) + " |")

    print("\n### Joins between one-second blocks (DRED q7, clean input)\n")
    print("| File | Spectral error at joins, dB | Elsewhere, dB | STOI, 1 s blocks | STOI, 0.2 s blocks | kB/min, 1 s | kB/min, 0.2 s |\n|---|---|---|---|---|---|---|")
    for n, j, r, s, s10, kb, kb10 in join_rows:
        print(f"| {n} | {j:.1f} | {r:.1f} | {s:.2f} | {s10:.2f} | {kb:.1f} | {kb10:.1f} |")

    if len(sys.argv) > 3:
        words(sys.argv[3], heard, ["Uncoded", "Uncoded, cut to 4 kHz"] + coders)

    # Speed and long-run level: all clean files joined, about a minute or more.
    long_in = os.path.join(out_dir, "long_in.raw")
    x = np.concatenate([load(os.path.join(src_dir, n + ".raw")) for n in names]); save(long_in, x)
    sec = len(x) / FS
    t = time.perf_counter(); run(DRED, "enc2", "7", str(EVERY), long_in, os.path.join(out_dir, "long.dred")); te = time.perf_counter() - t
    t = time.perf_counter(); run(DRED, "dec2", str(EVERY), os.path.join(out_dir, "long.dred"), os.path.join(out_dir, "long_out.raw")); td = time.perf_counter() - t
    y = load(os.path.join(out_dir, "long_out.raw")); lag = align(x, y); y = shifted(y, lag, len(x))
    third = len(x) // 3
    lv = [20 * np.log10(np.sqrt((y[i * third:(i + 1) * third] ** 2).mean()) / np.sqrt((x[i * third:(i + 1) * third] ** 2).mean())) for i in range(3)]
    print(f"\n### Speed and level on this computer, {sec:.0f} s of speech, DRED q7\n")
    print(f"- Encode: {te:.2f} s, {sec / te:.0f} times faster than real time. Decode: {td:.2f} s, {sec / td:.0f} times faster.")
    print(f"- Delay through coder and player: {lag / 16:.0f} ms.")
    print(f"- Output level against input in the first, middle and last third: {lv[0]:+.1f}, {lv[1]:+.1f}, {lv[2]:+.1f} dB.")

    # A sound that is not speech: does the player invent speech?
    n = FS * 6; tt = np.arange(n) / FS
    ns = 6000 * np.sin(2 * np.pi * (300 + 250 * np.floor(tt * 2)) * tt) * (np.floor(tt * 4) % 2) + 800 * pink(n, rng) / np.abs(pink(n, rng)).max()
    base = os.path.join(out_dir, "nonspeech"); save(base + "_in.raw", ns); wav(base + "_in.wav", ns)
    _, y = dred(base + "_in.raw", base + "_dred_q7", 7); wav(base + "_dred_q7.wav", y)
    print(f"- Non-speech test (beeps over hiss): output level {20 * np.log10(np.sqrt((y ** 2).mean()) / np.sqrt((ns ** 2).mean())):+.1f} dB against input. Listen to nonspeech_in.wav and nonspeech_dred_q7.wav.")


if __name__ == "__main__":
    main()
