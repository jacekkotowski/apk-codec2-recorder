package pl.dyktafon;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The recording path after the microphone: voice chain, voice activation, DRED coder, .dred file.
 * Used by RecService for the microphone and by MainActivity for compressing existing files, and runs in computer tests without Android.
 * Voice activation gates the sound before the coder. While closed it keeps 400 ms of processed sound, fed to the coder when the gate opens,
 * so the start of a word is not cut.
 */
final class Recorder implements AutoCloseable {
    /** The voice chain works in 40 ms frames, as it did for Codec 2 700C in 2.4, so its per-frame timings are unchanged. Two DRED frames each. */
    static final int CHUNK = 2 * Dred.FRAME;
    private static final int PRE = 400 / (CHUNK * 1000 / Dred.FS);

    final VoiceChain chain;
    private final DredLib.Coder coder; private final OutputStream out; private final boolean voiceOnly;
    private final short[][] pre = new short[PRE][CHUNK]; private int preW, preC; private boolean wasOpen;
    private final short[] half = new short[Dred.FRAME];
    /** Seconds of sound given to the coder, and bytes written so far. */
    volatile long frames, bytes;
    /** True while voice activation holds the recording back. */
    volatile boolean waiting;

    Recorder(OutputStream out, int q, boolean voiceOnly, VoiceChain chain) throws IOException {
        this.out = out; this.voiceOnly = voiceOnly; this.chain = chain;
        coder = new DredLib.Coder(q);
        out.write(Dred.header(q)); bytes = Dred.HEADER;
    }

    /** One chunk of CHUNK samples of 16 kHz sound, in place. Returns true if the voice detector heard voice. */
    boolean chunk(short[] pcm, int manualDb, int sensitivity, double hold) throws IOException {
        boolean voice = chain.process(pcm, manualDb, sensitivity, hold);
        boolean open = !voiceOnly || voice;
        waiting = !open;
        if (open) {
            if (!wasOpen) { for (int k = preC; k > 0; k--) code(pre[(preW - k + PRE) % PRE]); preC = 0; }
            code(pcm);
        } else {
            System.arraycopy(pcm, 0, pre[preW], 0, CHUNK); preW = (preW + 1) % PRE; if (preC < PRE) preC++;
        }
        wasOpen = open;
        return voice;
    }

    private void code(short[] pcm) throws IOException {
        for (int h = 0; h < 2; h++) {
            System.arraycopy(pcm, h * Dred.FRAME, half, 0, Dred.FRAME);
            int n = coder.frame(half); frames++;
            if (n >= 0) block(coder.buf, n);
        }
    }
    private void block(byte[] b, int n) throws IOException {
        Dred.writeBlock(out, b, n); bytes += 2 + n;
        out.flush(); // one block a second: what is written survives if the phone dies
    }

    /** Recorded length in seconds. */
    double seconds() { return frames * Dred.FRAME / (double) Dred.FS; }

    /** Completes the last block. The file is then a whole number of seconds long. */
    void finish() throws IOException { coder.finish(this::block); out.flush(); }

    @Override public void close() { coder.close(); }
}
