"""Radio link measurements on a computer. Needs tests/radio/build.sh to have run, python3 with numpy, and a speech file
(8 kHz, 16 bit raw, about 10 s) as the first argument for the live-voice part.
Sends a 2.6 kB file (30 s of 700C speech) with 50% repair frames through simulated channels, three random runs each.
python3 tests/radio/run.py SPEECH.raw"""
import os, subprocess, sys, numpy as np
here = os.path.dirname(os.path.abspath(__file__)); out = os.path.join(here, 'out'); os.chdir(out)
J = ['java', '-Djava.library.path=.', '-cp', '.', 'pl.dyktafon.RT']
def run(*a): return subprocess.run(list(a), capture_output=True, text=True)
def chan(src, *o): r = run('python3', os.path.join(here, 'chan.py'), src, 'c.raw', *map(str, o)); assert r.returncode == 0, r.stderr
open('msg.c2', 'wb').write(np.random.default_rng(7).integers(0, 256, 2600, dtype=np.uint8).tobytes())
NAMES = {0: 'Fast', 1: 'Robust', 2: 'Very robust'}
FILE_CASES = [
 ('Clean', []), ('Noise, SNR 10 dB', ['--snr', 10]), ('Noise, SNR 5 dB', ['--snr', 5]), ('Noise, SNR 2 dB', ['--snr', 2]), ('Noise, SNR 0 dB', ['--snr', 0]), ('Noise, SNR -3 dB', ['--snr', -3]), ('Noise, SNR -5 dB', ['--snr', -5]),
 ('HF fading, SNR 10 dB', ['--fade', '--snr', 10]), ('HF fading, SNR 5 dB', ['--fade', '--snr', 5]), ('HF fading, SNR 0 dB', ['--fade', '--snr', 0]),
 ('Speaker to mic, close, distorted, clocks 100 ppm apart, SNR 15 dB', ['--room', 10, '--clip', 2, '--ppm', 100, '--snr', 15]),
 ('Speaker to mic, echo 5 dB below direct, SNR 10 dB', ['--room', 5, '--snr', 10]),
 ('Speaker to mic, echo as loud as direct', ['--room', 0]),
 ('8 s gap in the middle', ['--drop', 8]),
]
print('## Files: 2.6 kB with 50% repair frames, received complete in N of 3 runs\n')
print('| Channel | ' + ' | '.join(NAMES.values()) + ' |'); print('|---|' + '---|' * 3)
secs = {}
for m in NAMES:
    r = run(*J, 'tx', str(m), 'msg.c2', 'm%d.raw' % m, '50'); secs[m] = r.stdout.split(', ')[-1].split(' s')[0]
rows = {c[0]: [] for c in FILE_CASES}
for m in NAMES:
    for name, o in FILE_CASES:
        ok = 0
        for seed in (1, 2, 3):
            chan('m%d.raw' % m, *o, '--seed', seed)
            if os.path.exists('back'): os.remove('back')
            r = run(*J, 'rx', str(m), 'c.raw', 'back')
            if r.returncode == 0 and open('back', 'rb').read() == open('msg.c2', 'rb').read(): ok += 1
        rows[name].append('%d/3' % ok)
for name, _ in FILE_CASES: print('| ' + name + ' | ' + ' | '.join(rows[name]) + ' |')
print('| Time on air, seconds | ' + ' | '.join(secs[m] for m in NAMES) + ' |')
# No signal at all: noise must never produce a frame.
(np.random.default_rng(3).standard_normal(8000 * 600) * 3000).astype('<i2').tofile('noise.raw')
print('\nFrames decoded from 10 minutes of pure noise: ' + ', '.join(NAMES[m] + ' ' + run(*J, 'rx', str(m), 'noise.raw', 'back').stdout.split('frames ok ')[1].split(',')[0] for m in NAMES))

if len(sys.argv) > 1:
    def env(x, w=160): n = len(x) // w; return np.log(np.abs(x[:n * w].astype(float)).reshape(n, w).sum(1) + 1)
    def match(a, b):  # loudness-contour agreement of two speech signals, best over a small shift
        best = 0
        for lag in range(-6, 30):
            x, y = (a[lag:], b) if lag >= 0 else (a, b[-lag:]); n = min(len(x), len(y))
            if n > 50: best = max(best, np.corrcoef(x[:n], y[:n])[0, 1])
        return best
    print('\n## Live voice: time in sync, and how well the decoded loudness contour matches the clean-channel decode (1.00 = same)\n')
    print('| Channel | 700D | 700E |'); print('|---|---|---|')
    VC = [('Clean', []), ('Noise, SNR 5 dB', ['--snr', 5]), ('Noise, SNR 2 dB', ['--snr', 2]), ('Noise, SNR 0 dB', ['--snr', 0]), ('Noise, SNR -2 dB', ['--snr', -2]), ('HF fading, SNR 5 dB', ['--fade', '--snr', 5]),
          ('Speaker to mic, close, distorted, clocks 100 ppm apart, SNR 15 dB', ['--room', 10, '--clip', 2, '--ppm', 100, '--snr', 15]), ('Speaker to mic, echo 5 dB below direct, SNR 10 dB', ['--room', 5, '--snr', 10]),
          ('Speaker to mic, close, with the decoded voice leaking into the mic 6 dB below the signal', ['--room', 10, '--speech', sys.argv[1], -6])]
    res = {c[0]: [] for c in VC}
    for m in (10, 11):
        run(*J, 'vtx', str(m), sys.argv[1], 'v.raw'); ref = None
        for name, o in VC:
            chan('v.raw', *o); r = run(*J, 'vrx', str(m), 'c.raw', 'o.raw'); e = env(np.fromfile('o.raw', dtype='<i2'))
            if ref is None: ref = e
            res[name].append(r.stdout.split('in sync ')[1].split(' of')[0] + ', ' + ('%.2f' % match(ref, e)))
    for name, _ in VC: print('| ' + name + ' | ' + ' | '.join(res[name]) + ' |')
