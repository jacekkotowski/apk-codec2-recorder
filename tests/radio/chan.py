"""Channel simulator for the radio tests. 8 kHz, 16 bit raw in and out.
python3 chan.py IN.raw OUT.raw [--snr dB] [--room D] [--clip X] [--ppm N] [--fade] [--drop SECONDS] [--speech FILE.raw LEVEL_dB] [--seed N]
--snr    white noise, signal-to-noise ratio in a 3 kHz band, measured against the signal while it is on
--room D phone speaker to microphone: small-speaker response (weak below 400 Hz), room echo with 0.3 s decay.
         D = direct sound against echo in dB: 10 = phone held at the microphone, 0 = phone about an arm's length away in a bare room
--clip   overdriven speaker or radio: soft clipping, X = drive (2 = clearly distorted)
--ppm    sound-card clock difference between sender and receiver, parts per million
--fade   HF-like fading: two paths 2 ms apart, each fading at about 1 Hz
--drop   one gap of this many seconds in the middle (someone bumps the phone)
--speech add another sound (decoded speech from the phone's own speaker) at LEVEL_dB relative to the signal"""
import sys, numpy as np
a = sys.argv; x = np.fromfile(a[1], dtype='<i2').astype(float)
def opt(name, n=1):
    if name in a:
        i = a.index(name); return a[i + 1:i + 1 + n] if n else True
    return None
rng = np.random.default_rng(int(opt('--seed')[0]) if opt('--seed') else 1)
on = np.abs(x) > 50; p_sig = np.mean(x[on] ** 2)
def filt(x, H):
    n = len(x); X = np.fft.rfft(x); f = np.fft.rfftfreq(n, 1 / 8000.0); return np.fft.irfft(X * H(f), n)
if opt('--clip'):
    d = float(opt('--clip')[0]); pk = np.max(np.abs(x)); x = np.tanh(x / pk * d) * pk / np.tanh(d)
if opt('--room'):
    x = filt(x, lambda f: (1j * f / 400) ** 2 / (1 + 1.4j * f / 400 + (1j * f / 400) ** 2) / (1 + (f / 3400) ** 8))
    n = 3200; t = np.arange(n) / 8000.0; h = rng.standard_normal(n) * 10 ** (-3 * t / 0.3); h[:16] = 0
    h *= 10 ** (-float(opt('--room')[0]) / 20) / np.sqrt(np.sum(h ** 2)); h[0] = 1
    x = np.convolve(x, h)[:len(x)]
if opt('--fade', 0):
    def ray(n):  # slowly changing complex gain, about 1 Hz wide
        m = n // 800 + 4; g = rng.standard_normal(m) + 1j * rng.standard_normal(m)
        k = np.hanning(9); g = np.convolve(g, k / np.sqrt(np.sum(k ** 2)), 'same')
        return np.interp(np.arange(n) / 800.0, np.arange(m), g.real) + 1j * np.interp(np.arange(n) / 800.0, np.arange(m), g.imag)
    X = np.fft.fft(x); X[len(X) // 2:] = 0; an = np.fft.ifft(2 * X)  # analytic signal
    d = np.concatenate([np.zeros(16), an[:-16]])
    x = np.real(an * ray(len(x)) + d * ray(len(x))) / np.sqrt(2)
if opt('--ppm'):
    r = 1 + float(opt('--ppm')[0]) * 1e-6; x = np.interp(np.arange(0, len(x) - 1, r), np.arange(len(x)), x)
if opt('--drop'):
    n = int(float(opt('--drop')[0]) * 8000); s = len(x) // 2; x[s:s + n] = 0
if opt('--speech'):
    f, lv = opt('--speech', 2); s = np.fromfile(f, dtype='<i2').astype(float); s = np.resize(s, len(x))
    x = x + s * np.sqrt(p_sig / np.mean(s ** 2)) * 10 ** (float(lv) / 20)
if opt('--snr'):
    p_n = p_sig / 10 ** (float(opt('--snr')[0]) / 10) * 4000 / 3000
    x = x + rng.standard_normal(len(x)) * np.sqrt(p_n)
x = x * min(1, 30000 / np.max(np.abs(x)))
np.clip(np.round(x), -32768, 32767).astype('<i2').tofile(a[2])
