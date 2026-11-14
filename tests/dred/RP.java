package pl.dyktafon;

import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** The app's recording path without AudioRecord (PLAN.md stage 9, part B, step 5): a sound file through Resampler, VoiceChain,
 *  voice activation and the DRED coder into a .dred file, with the app's default settings.
 *  java -Djava.library.path=tests/dred/out -cp tests/dred/out pl.dyktafon.RP Q DENOISE(0/1) VOICE_ACTIVATION(0/1) RATE in.raw out.dred [chain_out.raw]
 *  in.raw is 16 bit mono at RATE. chain_out.raw, if given, gets the sound as it goes into the coder (16 kHz). */
public class RP {
    public static void main(String[] a) throws Exception {
        int q = Integer.parseInt(a[0]); boolean dn = a[1].equals("1"), va = a[2].equals("1"); int rate = Integer.parseInt(a[3]);
        long inBytes = new File(a[4]).length(); InputStream s = new BufferedInputStream(new FileInputStream(a[4]), 65536); // streamed, so memory use is the app's
        OutputStream chainOut = a.length > 6 ? new BufferedOutputStream(new FileOutputStream(a[6])) : null;
        // Defaults of the page: medium denoise, light treble, medium rumble cut, lift, gate and compressor. -Dtreble= -Drumble= -Dlift= -Dgate= -Dcomp= -Dceiling= -Dmanual= change them for experiments.
        VoiceChain chain = new VoiceChain(Dred.FS, Double.parseDouble(System.getProperty("ceiling", "" + VoiceChain.CEILING_DRED)), Recorder.CHUNK, dn, 0.18, Integer.getInteger("treble", 1), null, Integer.getInteger("rumble", 180), Double.parseDouble(System.getProperty("lift", "6")), Double.parseDouble(System.getProperty("gate", "12")), Double.parseDouble(System.getProperty("comp", "3")));
        int manual = Integer.getInteger("manual", -100); // -100 is automatic level
        long t0 = System.nanoTime(); final int[] voiced = {0}, chunks = {0}; final double[] peak = {0};
        try (OutputStream o = new BufferedOutputStream(new FileOutputStream(a[5])); Recorder r = new Recorder(o, q, va, chain)) {
            short[] pcm = new short[Recorder.CHUNK]; int[] fill = {0};
            Resampler rs = new Resampler(rate, Dred.FS, v -> {
                pcm[fill[0]++] = (short) Math.max(-32767, Math.min(32767, Math.round(v)));
                if (fill[0] == pcm.length) {
                    fill[0] = 0;
                    if (r.chunk(pcm, manual, 5, 1)) voiced[0]++;
                    chunks[0]++;
                    for (short x : pcm) peak[0] = Math.max(peak[0], Math.abs(x));
                    if (chainOut != null) { ByteBuffer ob = ByteBuffer.allocate(pcm.length * 2).order(ByteOrder.LITTLE_ENDIAN); for (short x : pcm) ob.putShort(x); chainOut.write(ob.array()); }
                }
            });
            for (int lo, hi; (lo = s.read()) >= 0 && (hi = s.read()) >= 0; ) rs.put((short) (lo | hi << 8));
            r.finish();
            System.out.printf("seconds_in %.2f seconds_coded %.0f bytes %d chunks %d voiced %d chain_peak %.0f ceiling %.0f code_ms %.0f%n",
                    inBytes / 2.0 / rate, Dred.Index.of(new File(a[5])).seconds(), new File(a[5]).length(), chunks[0], voiced[0], peak[0], chain.ceiling, (System.nanoTime() - t0) / 1e6);
        }
        if (chainOut != null) chainOut.close();
    }
}
