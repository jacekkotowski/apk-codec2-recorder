#!/usr/bin/env python3
"""The app's recording path at 16 kHz, measured (PLAN.md stage 9, part B, step 5).

  python3 tests/dred/chain.py SPEECH_DIR OUT_DIR [Q]

SPEECH_DIR holds 16 kHz, 16 bit, mono .raw files. Needs tests/dred/build.sh to have run, numpy, scipy and pystoi.
Each file gets one second of its background before and after, as a recording would. Inputs: clean, pink noise 10 dB below
the speech, and white noise 5 dB below, each at normal level and 20 dB quieter (a distant or quiet microphone).
Versions: DRED alone (as in part A), and the app's recording path (tests/dred/RP.java: voice chain with the page's default
settings, then DRED) with Denoise off and on. Prints STOI against the clean original, and the level of the decoded sound.
"""
import os, subprocess, sys
import numpy as np
from pystoi import stoi

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
DRED = os.path.join(HERE, "dredtest")
JAVA = ["java", "-Djava.library.path=" + os.path.join(HERE, "out"), "-cp", os.path.join(HERE, "out"), "pl.dyktafon.RP"]
FS = 16000
sys.path.insert(0, HERE)
from run import align, shifted, pink, load, save, wav  # noqa: E402


def sh(*a):
    return subprocess.run(list(a), check=True, capture_output=True, text=True).stdout


def decode(dred, raw):
    blocks = dred + ".blocks"
    with open(dred, "rb") as f, open(blocks, "wb") as o:
        f.seek(8); o.write(f.read())  # dredtest reads the blocks without the app's header
    sh(DRED, "dec2", "50", blocks, raw)
    return load(raw)


def main():
    src, out = sys.argv[1], sys.argv[2]; q = sys.argv[3] if len(sys.argv) > 3 else "9"
    os.makedirs(out, exist_ok=True)
    rng = np.random.default_rng(5)
    names = sorted(f[:-4] for f in os.listdir(src) if f.endswith(".raw"))
    cases = [("clean", None, 0), ("clean, 20 dB quieter", None, -20), ("pink noise -10 dB", "pink", 0), ("pink noise -10 dB, 20 dB quieter", "pink", -20), ("white noise -5 dB", "white", 0)]
    versions = ["DRED alone", "Recording path, Denoise off", "Recording path, Denoise on"]
    res = {(c[0], v): [] for c in cases for v in versions}
    lv = {(c[0], v): [] for c in cases for v in versions[1:]}
    for name in names:
        speech = load(os.path.join(src, name + ".raw"))
        pad = np.zeros(FS)
        clean = np.concatenate([pad, speech, pad])
        for cname, kind, gain_db in cases:
            g = 10 ** (gain_db / 20)
            if kind is None:
                noise = rng.standard_normal(len(clean)) * 3  # a quiet microphone's own hiss, about -80 dB
                x = clean * g + noise
            else:
                nz = pink(len(clean), rng) if kind == "pink" else rng.standard_normal(len(clean))
                snr = 10 if kind == "pink" else 5
                nz *= np.sqrt((speech ** 2).mean() / (nz ** 2).mean()) * 10 ** (-snr / 20)
                x = (clean + nz) * g
            x = np.clip(x, -32767, 32767)
            base = os.path.join(out, f"{name}_{cname.replace(' ', '_').replace(',', '')}")
            save(base + "_in.raw", x)
            for v in versions:
                tag = {"DRED alone": "alone", "Recording path, Denoise off": "dn0", "Recording path, Denoise on": "dn1"}[v]
                if v == "DRED alone":
                    sh(DRED, "enc2", q, "50", base + "_in.raw", base + "_alone.blocks"); sh(DRED, "dec2", "50", base + "_alone.blocks", base + "_alone.raw")
                    y = load(base + "_alone.raw")
                else:
                    sh(*JAVA, q, tag[-1], "0", str(FS), base + "_in.raw", base + f"_{tag}.dred")
                    y = decode(base + f"_{tag}.dred", base + f"_{tag}.raw")
                    pk = np.abs(y).max(); clip = int((np.abs(y) >= 32000).sum())
                    rms = 20 * np.log10(np.sqrt((y[FS:-FS] ** 2).mean()) / 32768 + 1e-9)
                    lv[(cname, v)].append((pk, clip, rms))
                lag = align(clean, y)
                res[(cname, v)].append(stoi(clean, shifted(y, lag, len(clean)), FS, extended=False))
                if name == names[0]: wav(base + f"_{tag}.wav", y)
    print(f"### STOI against the clean original, DRED q{q}, {len(names)} files\n")
    print("| Input | " + " | ".join(versions) + " |\n|---|" + "---|" * len(versions))
    for c in cases:
        print(f"| {c[0]} | " + " | ".join(f"{np.mean(res[(c[0], v)]):.2f}" for v in versions) + " |")
    print(f"\n### Level of the decoded recording (recording path)\n")
    print("| Input | Denoise | Highest peak | Samples at full scale | Speech level, dBFS (mean, range) |\n|---|---|---|---|---|")
    for c in cases:
        for v in versions[1:]:
            a = lv[(c[0], v)]
            r = [x[2] for x in a]
            print(f"| {c[0]} | {'on' if v.endswith('on') else 'off'} | {max(x[0] for x in a):.0f} | {sum(x[1] for x in a)} | {np.mean(r):.1f} ({min(r):.1f} to {max(r):.1f}) |")


if __name__ == "__main__":
    main()
