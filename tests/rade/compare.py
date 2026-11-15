#!/usr/bin/env python3
"""Live voice on a computer: RADE V1 against Codec 2 700C in FreeDV 700D, through the radio channels of tests/radio/chan.py.

  python3 tests/rade/compare.py SPEECH_DIR OUT_DIR RADE_TX RADE_RX

SPEECH_DIR holds 16 kHz 16-bit mono .raw speech. RADE_TX and RADE_RX are rade_tx_wav and rade_rx_wav from rade_c (or the
app's build of them). 700D runs through tests/radio (bash tests/radio/build.sh first). Scores: STOI against the original
speech, and how long the decoded speech lags the original. Needs numpy, scipy, pystoi.
"""
import os, subprocess, sys, time, wave
import numpy as np
from scipy.signal import resample_poly
from pystoi import stoi

HERE = os.path.dirname(os.path.abspath(__file__)); ROOT = os.path.dirname(os.path.dirname(HERE))
RT = ["java", "-Djava.library.path=" + os.path.join(ROOT, "tests/radio/out"), "-cp", os.path.join(ROOT, "tests/radio/out"), "pl.dyktafon.RT"]
CHAN = os.path.join(ROOT, "tests/radio/chan.py")
CASES = [("Clean", []), ("Noise, SNR 5 dB", ["--snr", 5]), ("Noise, SNR 2 dB", ["--snr", 2]), ("Noise, SNR 0 dB", ["--snr", 0]),
         ("Noise, SNR -2 dB", ["--snr", -2]), ("Noise, SNR -4 dB", ["--snr", -4]), ("HF fading, SNR 5 dB", ["--fade", "--snr", 5]),
         ("HF fading, SNR 2 dB", ["--fade", "--snr", 2]),
         ("Speaker to mic, close, distorted, clocks 100 ppm apart, SNR 15 dB", ["--room", 10, "--clip", 2, "--ppm", 100, "--snr", 15]),
         ("Speaker to mic, echo 5 dB below direct, SNR 10 dB", ["--room", 5, "--snr", 10]),
         ("Speaker to mic, echo as loud as direct", ["--room", 0])]


def sh(*a): return subprocess.run([str(x) for x in a], check=True, capture_output=True, text=True)
def load(p): return np.fromfile(p, dtype="<i2").astype(float)
def save(p, x): np.clip(np.round(x), -32768, 32767).astype("<i2").tofile(p)


def wav_w(p, x, fs):
    with wave.open(p, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(fs)
        w.writeframes(np.clip(np.round(x), -32768, 32767).astype("<i2").tobytes())


def wav_r(p):
    with wave.open(p, "rb") as w: return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(float)


def score(ref, y):
    """Lag of y against ref from the loudness contour in 10 ms steps, up to 1.5 s, then STOI on the aligned signal."""
    env = lambda a: np.log(np.sqrt((a[:len(a) // 160 * 160].reshape(-1, 160) ** 2).mean(1)) + 10)
    a, b = env(ref), env(y); n = len(a) - 200
    best, lag = -2, 0
    for d in range(0, 150):
        if 50 + d + n - 50 > len(b): break
        c = np.corrcoef(a[50:n], b[50 + d:n + d])[0, 1]
        if c > best: best, lag = c, d
    s = y[lag * 160:]; s = np.concatenate([s, np.zeros(max(0, len(ref) - len(s)))])[:len(ref)]
    return stoi(ref, s, 16000), lag * 10


def main():
    src, out, tx, rx = sys.argv[1:5]; os.makedirs(out, exist_ok=True)
    names = sorted(f for f in os.listdir(src) if f.endswith(".raw"))[:6]
    speech = np.concatenate([np.concatenate([load(os.path.join(src, n)), np.zeros(8000)]) for n in names])
    speech = np.concatenate([np.zeros(16000), speech, np.zeros(16000)])
    p = lambda n: os.path.join(out, n)
    wav_w(p("in.wav"), speech, 16000); save(p("in8.raw"), resample_poly(speech, 1, 2))
    t = time.perf_counter(); sh(tx, "-v", "0", p("in.wav"), p("rade_tx.wav")); t_tx = time.perf_counter() - t
    save(p("rade_tx.raw"), wav_r(p("rade_tx.wav")))
    sh(*RT, "vtx", 10, p("in8.raw"), p("d_tx.raw"))
    secs = len(speech) / 16000
    print(f"### Live voice, {secs:.0f} s of English speech ({len(names)} files), STOI against the original (1.00 = all understood) and delay\n")
    print("| Channel | RADE V1 | Codec 2 700C in 700D |\n|---|---|---|")
    t_rx = None
    for name, o in CASES:
        cells = []
        for kind in ("rade", "d"):
            sh("python3", CHAN, p(kind + "_tx.raw"), p("c.raw"), *o)
            if kind == "rade":
                wav_w(p("c.wav"), load(p("c.raw")), 8000)
                t = time.perf_counter(); sh(rx, "-v", "0", p("c.wav"), p("rade_rx.wav"))
                if t_rx is None: t_rx = time.perf_counter() - t
                y = wav_r(p("rade_rx.wav"))
                wav_w(p("rade_" + name.split(",")[0].replace(" ", "_") + ".wav"), y, 16000)
            else:
                sh(*RT, "vrx", 10, p("c.raw"), p("d_rx.raw")); y = resample_poly(load(p("d_rx.raw")), 2, 1)
            s, lag = score(speech, y)
            cells.append(f"{s:.2f}, {lag} ms")
        print(f"| {name} | {cells[0]} | {cells[1]} |")
    print(f"\nRADE on this computer, generic flags: coding {secs / t_tx:.0f} times faster than real time, decoding {secs / t_rx:.0f} times.")


if __name__ == "__main__":
    main()
