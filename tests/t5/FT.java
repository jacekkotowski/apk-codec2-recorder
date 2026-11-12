package pl.dyktafon;

import java.util.*;

/** Fountain transport test. Run from the project folder:
 *  javac -d tests/t5 app/src/pl/dyktafon/Fountain.java tests/t5/FT.java && java -cp tests/t5 pl.dyktafon.FT */
public class FT {
    static int fails = 0;
    static void check(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL " + what); } }

    /** Sends k plain frames and `extra` repair frames through a channel that loses `loss` of them. Returns frames used, or -1. */
    static int run(Random r, int frame, int size, double loss, double extra, boolean shuffle) {
        byte[] file = new byte[size]; r.nextBytes(file);
        Fountain.Tx tx = new Fountain.Tx("zażółć_gęślą.c2", file, frame, r.nextInt());
        int n = tx.k + (int) Math.ceil(tx.k * extra) + 25; // the margin keeps tiny files from failing by plain bad luck
        List<byte[]> sent = new ArrayList<>();
        for (int i = 0; i < n; i++) if (r.nextDouble() >= loss) { sent.add(tx.frame(i)); if (r.nextInt(10) == 0) sent.add(tx.frame(i)); } // some duplicates
        if (shuffle) Collections.shuffle(sent, r);
        Fountain.Rx rx = new Fountain.Rx(frame);
        int used = 0;
        for (byte[] f : sent) {
            used++;
            if (r.nextInt(20) == 0) { byte[] junk = new byte[frame]; r.nextBytes(junk); rx.add(junk); } // a stray frame from somewhere else
            if (rx.add(f)) break;
        }
        if (!rx.done) return -1;
        check(Arrays.equals(rx.file, file), "content " + frame + "/" + size);
        check(rx.name.equals("zażółć_gęślą.c2"), "name");
        return used;
    }

    public static void main(String[] a) {
        Random r = new Random(1);
        for (int frame : new int[]{54, 126, 510})
            for (int size : new int[]{0, 1, 100, 2600, 26000})
                for (double loss : new double[]{0, 0.1, 0.3, 0.5}) {
                    int bad = 0;
                    for (int t = 0; t < 20; t++) if (run(r, frame, size, loss, loss == 0 ? 0 : 2 * loss / (1 - loss) + 0.3, t % 2 == 1) < 0) bad++;
                    check(bad == 0, "frame " + frame + " size " + size + " loss " + loss + ": " + bad + "/20 not received");
                }
        // Exactly k frames, all of them repair frames: how often is one more needed?
        int more = 0, T = 2000;
        for (int t = 0; t < T; t++) {
            byte[] file = new byte[1000]; r.nextBytes(file);
            Fountain.Tx tx = new Fountain.Tx("x", file, 126, t);
            Fountain.Rx rx = new Fountain.Rx(126);
            for (int i = 0; i < tx.k; i++) rx.add(tx.frame(tx.k + i));
            if (!rx.done) { more++; rx.add(tx.frame(2 * tx.k)); check(rx.done, "one extra frame was not enough"); }
        }
        System.out.println("needed one frame more than k: " + more + " of " + T);
        // A second transmission replaces the first only after two of its frames.
        byte[] f1 = new byte[500], f2 = new byte[700]; r.nextBytes(f1); r.nextBytes(f2);
        Fountain.Tx t1 = new Fountain.Tx("one", f1, 126, 11), t2 = new Fountain.Tx("two", f2, 126, 22);
        Fountain.Rx rx = new Fountain.Rx(126);
        for (int i = 0; i < t1.k; i++) rx.add(t1.frame(i));
        check(rx.done && rx.name.equals("one"), "first file");
        rx.add(t2.frame(0)); check(rx.name.equals("one"), "one stray frame must not reset");
        for (int i = 1; i < t2.k; i++) rx.add(t2.frame(i));
        check(rx.done && rx.name.equals("two") && Arrays.equals(rx.file, f2), "second file");
        // Speed: a 5 minute 700C recording (26 kB) and a large 200 kB file in the slow 126-byte mode, half the frames lost.
        for (int size : new int[]{26000, 200000}) { long t0 = System.nanoTime(); int u = run(r, 126, size, 0.5, 2.2, true); System.out.println(size + " bytes, 50% loss: " + (u < 0 ? "not received" : u + " frames") + ", " + (System.nanoTime() - t0) / 1000000 + " ms incl. sending side"); check(u > 0, "big"); }
        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
