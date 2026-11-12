package pl.dyktafon;

/** Sends and receives a file as modem audio: Fountain frames inside FreeDV data bursts. No Android code here, so it runs in computer tests. */
final class RadioLink {
    static final int BURST = 4; // frames per burst; sender and receiver must agree
    interface Out { void write(short[] s, int n) throws Exception; }
    interface Stop { boolean now(); }

    /** A steady tone: opens the transmitter's VOX before the data, and serves for setting the level. */
    static void tone(Out out, double seconds, double amp) throws Exception {
        short[] b = new short[800]; int total = (int) (seconds * 8000);
        for (int done = 0; done < total; done += b.length) {
            int n = Math.min(b.length, total - done);
            for (int i = 0; i < n; i++) { double fade = Math.min(1, Math.min(done + i, total - done - i) / 80.0); b[i] = (short) (amp * fade * Math.sin(2 * Math.PI * 1500 * (done + i) / 8000.0)); }
            out.write(b, n);
        }
    }

    static final class Sender {
        private final long h; private final Fountain.Tx tx; private final short[] buf; private final int burstSamples;
        final int k, frames; volatile int sent;

        /** extraPct: repair frames as a percentage of the file's own frames. */
        Sender(int mode, String name, byte[] file, int extraPct) {
            h = Radio.open(mode, BURST);
            if (h == 0) throw new IllegalStateException("the modem did not start");
            tx = new Fountain.Tx(name, file, Radio.payload(h), (int) (System.nanoTime() ^ file.length * 31L));
            k = tx.k; buf = new short[Radio.txMax(h)];
            int want = k + (k * extraPct + 99) / 100;
            frames = (want + BURST - 1) / BURST * BURST; // whole bursts: the spare places carry more repair frames
            burstSamples = Radio.preamble(h, buf) + BURST * Radio.frameN(h) + Radio.postamble(h, buf) + Radio.gap(h);
        }
        double seconds() { return 1.0 + frames / BURST * (burstSamples / 8000.0); }

        /** Plays the whole transmission into out. With repeat it keeps sending new repair frames until stopped. */
        void run(Out out, Stop stop, boolean repeat) throws Exception {
            tone(out, 1.0, 8000);
            int index = 0;
            while (!stop.now() && (repeat || index < frames) && index + BURST <= 65536) {
                out.write(buf, Radio.preamble(h, buf));
                for (int i = 0; i < BURST; i++) { out.write(buf, Radio.frame(h, tx.frame(index++), buf)); sent = index; }
                out.write(buf, Radio.postamble(h, buf));
                java.util.Arrays.fill(buf, (short) 0); out.write(buf, Radio.gap(h));
            }
        }
        void close() { Radio.close(h); }
    }

    static final class Receiver {
        private final long h; private final int payload; private final byte[] out; private final Fountain.Rx rx;
        volatile int framesOk, got, needed, sync; volatile float snr; volatile boolean done; volatile String name; volatile byte[] file;

        Receiver(int mode) {
            h = Radio.open(mode, BURST);
            if (h == 0) throw new IllegalStateException("the modem did not start");
            payload = Radio.payload(h); out = new byte[payload * 8]; rx = new Fountain.Rx(payload);
        }
        /** Give it the microphone samples as they arrive. Returns true when a new file has just been completed; name and file then hold it. */
        boolean feed(short[] s, int n) {
            int f = Radio.rxData(h, s, n, out); boolean fresh = false;
            for (int i = 0; i < f; i++) {
                framesOk++;
                if (rx.add(java.util.Arrays.copyOfRange(out, i * payload, (i + 1) * payload)) && rx.file != file) { name = rx.name; file = rx.file; fresh = true; }
            }
            done = rx.done; got = rx.got(); needed = rx.needed(); sync = Radio.sync(h); snr = Radio.snr(h);
            return fresh;
        }
        void close() { Radio.close(h); }
    }
}
