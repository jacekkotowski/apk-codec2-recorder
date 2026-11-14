package pl.dyktafon;

import java.io.*;
import java.util.*;

/** .dred format test: write blocks, read them back, seek, and refuse files the app can't play. Run from the project folder:
 *  javac -d tests/t6/out app/src/pl/dyktafon/Dred.java tests/t6/DT.java && java -cp tests/t6/out pl.dyktafon.DT */
public class DT {
    static int fails = 0;
    static void check(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL " + what); } }
    static String refusal(byte[] data) { try { Dred.Index.of(new ByteArrayInputStream(data)); return null; } catch (IOException e) { return e.getMessage(); } }

    public static void main(String[] a) throws Exception {
        Random r = new Random(1);
        File f = File.createTempFile("dredtest", ".dred"); f.deleteOnExit();
        byte[][] blocks = new byte[300][];
        try (OutputStream o = new BufferedOutputStream(new FileOutputStream(f))) {
            o.write(Dred.header(9));
            for (int i = 0; i < blocks.length; i++) {
                int n = i % 37 == 5 ? 0 : i == 7 ? Dred.MAX_BLOCK : 20 + r.nextInt(120);
                blocks[i] = new byte[n]; r.nextBytes(blocks[i]);
                Dred.writeBlock(o, blocks[i], n);
            }
        }
        Dred.Index x = Dred.Index.of(f);
        check(x.q == 9 && x.blocks == 300 && x.seconds() == 300, "header and count: q " + x.q + " blocks " + x.blocks);
        byte[] b = new byte[Dred.MAX_BLOCK];
        try (RandomAccessFile in = new RandomAccessFile(f, "r")) {
            for (int i : new int[]{299, 0, 150, 5, 7, 42, 298}) { // seek in any order
                int n = x.read(in, i, b);
                check(n == blocks[i].length && Arrays.equals(Arrays.copyOf(b, n), blocks[i]), "block " + i);
            }
        }
        check(Dred.nameOf(9).equals("small") && Dred.qOf("smallest") == 12 && Dred.qOf("700C") == -1 && Dred.nameOf(3).equals("q3"), "level names");

        byte[] all = java.nio.file.Files.readAllBytes(f.toPath());
        // A recording cut off in the middle of a block (the phone died while writing) keeps every whole block.
        for (int cut : new int[]{1, 2, 3, blocks[299].length + 1}) {
            byte[] part = Arrays.copyOf(all, all.length - cut);
            Dred.Index p = Dred.Index.of(new ByteArrayInputStream(part));
            check(p.blocks == 299, "cut by " + cut + " bytes: " + p.blocks + " blocks");
        }
        check(Dred.Index.of(new ByteArrayInputStream(Dred.header(4))).blocks == 0, "header only");

        String m;
        byte[] bad = all.clone(); bad[0] = 'X';
        check((m = refusal(bad)) != null && m.contains("isn't a .dred"), "wrong magic: " + m);
        bad = all.clone(); bad[5] = 13;
        check((m = refusal(bad)) != null && m.contains("different version of DRED"), "unknown bitstream: " + m);
        bad = all.clone(); bad[4] = 2;
        check((m = refusal(bad)) != null && m.contains("different version"), "unknown format: " + m);
        bad = all.clone(); bad[6] = 16;
        check((m = refusal(bad)) != null && m.contains("damaged header"), "q out of range: " + m);
        bad = all.clone(); bad[7] = 10;
        check((m = refusal(bad)) != null && m.contains("damaged header"), "frames per block: " + m);
        bad = all.clone(); bad[Dred.HEADER] = (byte) 0x7f;
        check((m = refusal(bad)) != null && m.contains("damaged"), "block too long: " + m);
        byte[] c2 = {(byte) 0xc0, (byte) 0xde, (byte) 0xc2, 1, 0, 8, 0, 1, 2, 3, 4, 5};
        check((m = refusal(c2)) != null && m.contains("Codec 2") && m.contains("2.6"), "old .c2 file: " + m);
        check((m = refusal(Arrays.copyOf(c2, 7))) != null && m.contains("Codec 2"), "old .c2 file, header only: " + m);
        check((m = refusal(new byte[3])) != null, "too short");

        System.out.println(fails == 0 ? "DRED FORMAT OK" : fails + " FAILURES"); System.exit(fails == 0 ? 0 : 1);
    }
}
