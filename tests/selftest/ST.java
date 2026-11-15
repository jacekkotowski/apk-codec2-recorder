package pl.dyktafon;

/** Computer test of SelfTest.java: the speed tests run on this computer's libraries, and the modem loop runs through a simulated
 *  speaker-and-microphone path. A good path must deliver the file at every level; a clipped, noisy or silent path must be reported as a failure,
 *  so the report can't say "received" when it was not. Run with tests/selftest/run.sh. */
public class ST {
    static int fails = 0;
    static void check(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL " + what); } else System.out.println("ok   " + what); }

    /** The simulated path: gain, optional hard clipping at the microphone, noise, a delay, a lead of silence and a tail. */
    static SelfTest.Path sim(double micGain, double noiseRms, int clipAt, boolean silent) {
        return (tx, n, gain) -> {
            java.util.Random r = new java.util.Random(3);
            int lead = 8000, tail = 12000, delay = 640; short[] mic = new short[lead + delay + n + tail];
            for (int i = 0; i < mic.length; i++) {
                double v = noiseRms * r.nextGaussian();
                int k = i - lead - delay; if (!silent && k >= 0 && k < n) v += tx[k] * gain * micGain;
                mic[i] = (short) Math.max(-clipAt, Math.min(clipAt, Math.round(v)));
            }
            return new SelfTest.Heard(mic, lead);
        };
    }
    static final SelfTest.Log LOG = new SelfTest.Log() { public void line(String s) { System.out.println("   | " + s); } public boolean cancelled() { return false; } };

    public static void main(String[] a) throws Exception {
        SelfTest.dredSpeed(LOG, 10);
        SelfTest.radeSpeed(LOG, 6);
        boolean[] ok = {false};
        for (double g : new double[]{0.25, 0.5, 1.0}) { SelfTest.modemRun(LOG, sim(1.0, 30, 32767, false), 1, g, ok); check(ok[0], "clean path, robust, level " + g); }
        for (int m = 0; m < 3; m++) { SelfTest.modemRun(LOG, sim(1.0, 30, 32767, false), m, 0.5, ok); check(ok[0], "clean path, mode " + m); }
        SelfTest.modemRun(LOG, sim(1.0, 12000, 32767, false), 0, 0.5, ok); check(!ok[0], "very noisy path fails the fast mode");
        SelfTest.modemRun(LOG, sim(1.0, 30, 32767, true), 1, 0.5, ok); check(!ok[0], "silent microphone: not received");
        // A microphone that overloads: the modem sound at 4 times the level, clipped hard. The report must say CLIPPED.
        java.io.ByteArrayOutputStream cap = new java.io.ByteArrayOutputStream(); java.io.PrintStream old = System.out;
        System.setOut(new java.io.PrintStream(cap)); SelfTest.modemRun(LOG, sim(4.0, 30, 32767, false), 1, 1.0, ok); System.setOut(old);
        check(cap.toString().contains("CLIPPED"), "clipping is reported");
        System.out.print(cap);
        java.util.ArrayList<short[]> heard = SelfTest.modemLoop(LOG, sim(1.0, 30, 32767, false)); check(heard.size() == 5, "modem loop makes five runs");
        short[] clip = new short[16000 * 5]; java.util.Arrays.fill(clip, (short) 100); clip[40000] = 32767;
        String cs = SelfTest.clipSummary(clip, 16000); System.out.println("   | " + cs);
        check(cs.contains("peak 0 dBFS") && cs.contains("5 s") && cs.contains("CLIPPED 1 samples"), "clip summary");
        System.out.println(fails == 0 ? "ALL PASS" : fails + " FAILED"); System.exit(fails == 0 ? 0 : 1);
    }
}
