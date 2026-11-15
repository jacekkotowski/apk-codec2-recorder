package pl.dyktafon;

/** RADE V1, the FreeDV project's neural voice mode, in libvoice.so (app/jni/radejni.c). Speech is 16 kHz, modem sound 8 kHz.
 *  One context does one job at a time: send a message, or receive. */
final class Rade implements AutoCloseable {
    static { System.loadLibrary("voice"); }
    private static native long open();
    private static native void close(long h);
    private static native short[] send(long h, short[] speech, int n, int leadMs);
    private static native int receive(long h, short[] in, int n, short[] out, int cap);
    private static native boolean sync(long h);
    private static native float snr(long h);
    private static native int endOfOver(long h);

    static final int SPEECH_FS = 16000, MODEM_FS = 8000;
    /** Receive in steps of this many modem samples (100 ms); out then needs OUT_PER_STEP samples. */
    static final int STEP = 800, OUT_PER_STEP = 4 * 1920;

    private long h;
    Rade() { h = open(); if (h == 0) throw new IllegalStateException("RADE did not start"); }

    /** Modem sound for a whole message: leadMs of coded silence (opens a transmitter's VOX), the speech, then the end-of-over frame. */
    short[] send(short[] speech, int n, int leadMs) { short[] s = send(h, speech, n, leadMs); if (s == null) throw new IllegalStateException("RADE could not code the message"); return s; }
    /** Radio sound in, at most STEP samples; speech out into out (OUT_PER_STEP long). Returns the speech samples. */
    int receive(short[] in, int n, short[] out) { return receive(h, in, n, out, out.length); }
    boolean sync() { return sync(h); }
    float snr() { return snr(h); }
    /** End-of-over frames seen since the last call: the sender finished. */
    int endOfOver() { return endOfOver(h); }
    @Override public void close() { if (h != 0) close(h); h = 0; }
}
