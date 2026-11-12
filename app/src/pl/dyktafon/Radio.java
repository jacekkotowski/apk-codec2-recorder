package pl.dyktafon;

/** FreeDV modems from Codec 2 1.2 (app/jni/fdvjni.c). All audio is 8 kHz, 16 bit, mono. */
final class Radio {
    static { System.loadLibrary("fdv"); }
    /** Modes: 0 fast, 1 robust, 2 very robust (files); 10 FreeDV 700D, 11 FreeDV 700E (live voice). */
    static native long open(int mode, int framesPerBurst);
    static native void close(long h);
    // Files
    static native int payload(long h);
    static native int txMax(long h);
    static native int frameN(long h);
    static native int preamble(long h, short[] out);
    static native int frame(long h, byte[] payload, short[] out);
    static native int postamble(long h, short[] out);
    static native int gap(long h);
    /** Takes n samples; writes whole good frames to out, returns how many. */
    static native int rxData(long h, short[] in, int n, byte[] out);
    // Live voice
    static native int speechN(long h);
    static native int modemN(long h);
    static native void txVoice(long h, short[] speech, short[] mod);
    static native int rxVoice(long h, short[] in, int n, short[] out);
    static native int sync(long h);
    static native float snr(long h);
}
