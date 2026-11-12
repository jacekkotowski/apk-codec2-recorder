package pl.dyktafon;

import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** Radio link tool for computer tests.
 *  tx MODE FILE OUT.raw EXTRA_PCT    file to modem audio (8 kHz, 16 bit)
 *  rx MODE IN.raw OUT_FILE           modem audio back to a file; prints what it got
 *  vtx MODE SPEECH.raw OUT.raw       live voice: speech to modem audio
 *  vrx MODE IN.raw SPEECH.raw        live voice: modem audio to speech */
public class RT {
    static short[] read(String p) throws IOException { byte[] b = Files.readAllBytes(Paths.get(p)); short[] s = new short[b.length / 2]; ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s); return s; }
    static void write(DataOutputStream o, short[] s, int n) throws IOException { byte[] b = new byte[n * 2]; ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(s, 0, n); o.write(b); }

    public static void main(String[] a) throws Exception {
        int mode = Integer.parseInt(a[1]);
        if (a[0].equals("tx")) {
            byte[] file = Files.readAllBytes(Paths.get(a[2]));
            RadioLink.Sender s = new RadioLink.Sender(mode, new File(a[2]).getName(), file, Integer.parseInt(a[4]));
            long[] n = {0};
            try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(a[3])))) { s.run((b, c) -> { write(o, b, c); n[0] += c; }, () -> false, false); }
            System.out.printf("tx: %d bytes, k=%d, frames=%d, %.1f s (estimate %.1f s)%n", file.length, s.k, s.frames, n[0] / 8000.0, s.seconds());
            s.close();
        } else if (a[0].equals("rx")) {
            short[] in = read(a[2]); RadioLink.Receiver r = new RadioLink.Receiver(mode);
            int at = -1; short[] chunk = new short[1024]; // odd-sized chunks, as a microphone would give
            for (int p = 0; p < in.length; p += 1000) { int n = Math.min(1000, in.length - p); System.arraycopy(in, p, chunk, 0, n); if (r.feed(chunk, n) && at < 0) at = p; }
            boolean ok = r.file != null;
            System.out.printf("rx: frames ok %d, got %d of %d, %s%s%n", r.framesOk, r.got, r.needed, ok ? "COMPLETE " + r.name + " at " + at / 8000 + " s" : "INCOMPLETE", "");
            if (ok) Files.write(Paths.get(a[3]), r.file);
            r.close(); System.exit(ok ? 0 : 2);
        } else if (a[0].equals("vtx")) {
            long h = Radio.open(mode, 1); int ns = Radio.speechN(h), nm = Radio.modemN(h); short[] in = read(a[2]), sp = new short[ns], mod = new short[nm];
            try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(a[3])))) { for (int p = 0; p + ns <= in.length; p += ns) { System.arraycopy(in, p, sp, 0, ns); Radio.txVoice(h, sp, mod); write(o, mod, nm); } }
            System.out.println("vtx: speech frame " + ns + ", modem frame " + nm); Radio.close(h);
        } else {
            long h = Radio.open(mode, 1); short[] in = read(a[2]), out = new short[16000], chunk = new short[1000]; long synced = 0, total = 0; double snr = 0;
            try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(a[3])))) {
                for (int p = 0; p < in.length; p += 1000) { int n = Math.min(1000, in.length - p); System.arraycopy(in, p, chunk, 0, n); int m = Radio.rxVoice(h, chunk, n, out); write(o, out, m); total++; if (Radio.sync(h) != 0) { synced++; snr += Radio.snr(h); } }
            }
            System.out.printf("vrx: in sync %.0f%% of the time, SNR %.1f dB%n", 100.0 * synced / total, synced > 0 ? snr / synced : 0); Radio.close(h);
        }
    }
}
