package pl.dyktafon;

/** Converts audio of any sample rate to 8 kHz mono with an anti-alias filter. */
final class Resampler {
    interface Sink { void sample(double v) throws Exception; }
    private final double[] taps, buf; private int p; private final double step; private double t; private double prev;
    private final Sink sink;

    Resampler(int rate, Sink sink) {
        this.sink = sink; step = rate / 8000.0;
        int N = Math.max(31, (int) (rate / 8000.0 * 32)) | 1; taps = new double[N]; buf = new double[N];
        double fc = 3600.0 / rate, sum = 0;
        for (int i = 0; i < N; i++) { double m = i - (N - 1) / 2.0; taps[i] = (m == 0 ? 2 * fc : Math.sin(2 * Math.PI * fc * m) / (Math.PI * m)) * (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (N - 1))); sum += taps[i]; }
        for (int i = 0; i < N; i++) taps[i] /= sum;
    }

    void put(double v) throws Exception {
        buf[p] = v; p = (p + 1) % buf.length;
        t += 1;
        if (t >= step) {
            t -= step;
            double y = 0; int q = p;
            for (double c : taps) { y += c * buf[q]; if (++q == buf.length) q = 0; }
            sink.sample(y);
        }
    }
}
