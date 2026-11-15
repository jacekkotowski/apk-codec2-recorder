package pl.dyktafon;

import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** The app's RADE layer (Rade.java, app/jni/radejni.c) without Android.
 *  java -Djava.library.path=tests/dred/out -cp tests/dred/out pl.dyktafon.RA tx LEAD_MS speech16k.raw modem8k.raw
 *  java -Djava.library.path=tests/dred/out -cp tests/dred/out pl.dyktafon.RA rx modem8k.raw speech16k.raw
 *  tx codes a whole message as RadioService sends it; rx feeds the modem sound in steps of 100 ms, as RadioService listens.
 *  With LEAD_MS 0, tx must match rade_tx_wav, and rx must match rade_rx_wav on the same input. */
public class RA {
    static short[] load(String p) throws IOException {
        byte[] b = Files.readAllBytes(Paths.get(p)); short[] s = new short[b.length / 2];
        ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s); return s;
    }
    static void save(String p, short[] s, int n) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN); b.asShortBuffer().put(s, 0, n); Files.write(Paths.get(p), b.array());
    }

    public static void main(String[] a) throws Exception {
        long t0 = System.nanoTime();
        try (Rade r = new Rade()) {
            if (a[0].equals("tx")) {
                short[] sp = load(a[2]); short[] m = r.send(sp, sp.length, Integer.parseInt(a[1]));
                save(a[3], m, m.length);
                System.out.printf("tx speech %.2f s, modem %.2f s, %.0f ms%n", sp.length / 16000.0, m.length / 8000.0, (System.nanoTime() - t0) / 1e6);
            } else {
                short[] m = load(a[1]); short[] out = new short[Rade.OUT_PER_STEP], step = new short[Rade.STEP];
                ByteArrayOutputStream bo = new ByteArrayOutputStream(); int synced = 0, steps = 0, eoo = 0;
                for (int i = 0; i < m.length; i += Rade.STEP) {
                    int n = Math.min(Rade.STEP, m.length - i); System.arraycopy(m, i, step, 0, n);
                    int k = r.receive(step, n, out);
                    ByteBuffer b = ByteBuffer.allocate(k * 2).order(ByteOrder.LITTLE_ENDIAN); b.asShortBuffer().put(out, 0, k); bo.write(b.array());
                    steps++; if (r.sync()) synced++; eoo += r.endOfOver();
                }
                Files.write(Paths.get(a[2]), bo.toByteArray());
                System.out.printf("rx modem %.2f s, speech %.2f s, in sync %d%% of the time, end of over seen %d, %.0f ms%n", m.length / 8000.0, bo.size() / 32000.0, 100 * synced / steps, eoo, (System.nanoTime() - t0) / 1e6);
            }
        }
    }
}
