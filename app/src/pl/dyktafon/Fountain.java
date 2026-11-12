package pl.dyktafon;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.CRC32;

/**
 * File transport for one-way radio links. The file is cut into blocks that fit one modem frame. After the
 * plain blocks the sender can make any number of repair blocks; each is a random mix of all plain blocks
 * (random linear code over GF(256)). The receiver rebuilds the file from any k different frames, in any
 * order, where k is the number of plain blocks. About 1 time in 250 it needs one frame more.
 *
 * Frame: id (2 bytes), k (2), index (2), block. index below k is a plain block, otherwise a repair block.
 * The blocks together hold: file length (4), CRC32 of the file (4), name length (1), name, file, zero padding.
 * The modem is expected to drop damaged frames (FreeDV data modes carry a CRC16 per frame).
 */
final class Fountain {
    static final int HEAD = 6, MAX_K = 20000, MAX_NAME = 80;
    private static final int[] EXP = new int[510], LOG = new int[256];
    static {
        int x = 1;
        for (int i = 0; i < 255; i++) { EXP[i] = x; LOG[x] = i; x <<= 1; if (x >= 256) x ^= 0x11d; }
        for (int i = 255; i < 510; i++) EXP[i] = EXP[i - 255];
    }
    private static int mul(int a, int b) { return a == 0 || b == 0 ? 0 : EXP[LOG[a] + LOG[b]]; }
    private static int inv(int a) { return EXP[255 - LOG[a]]; }

    /** dst ^= c * src over GF(256). */
    private static void addMul(byte[] dst, int d, byte[] src, int s, int len, int c) {
        if (c == 0) return;
        if (c == 1) { for (int i = 0; i < len; i++) dst[d + i] ^= src[s + i]; return; }
        int lc = LOG[c];
        for (int i = 0; i < len; i++) { int v = src[s + i] & 255; if (v != 0) dst[d + i] ^= (byte) EXP[lc + LOG[v]]; }
    }

    /** Mixing weights of repair block `index`. Sender and receiver must produce the same bytes. */
    private static void coeffs(int id, int index, byte[] out, int k) {
        // Must not be a linear generator such as xorshift: its rows would depend on each other and the file could never be solved.
        int s = (id + 1) * 0x9E3779B1 ^ (index + 1) * 0x85EBCA6B;
        for (int i = 0; i < k; i++) {
            s += 0x9E3779B9; int z = s;
            z ^= z >>> 16; z *= 0x85EBCA6B; z ^= z >>> 13; z *= 0xC2B2AE35; z ^= z >>> 16;
            out[i] = (byte) (z >>> 24);
        }
    }

    static final class Tx {
        final int k, id; private final int frameBytes, block; private final byte[] data;

        /** frameBytes is the modem's payload size per frame. id tells transmissions apart; pick it at random. */
        Tx(String name, byte[] file, int frameBytes, int id) {
            byte[] nm = name.getBytes(StandardCharsets.UTF_8);
            if (nm.length > MAX_NAME) { nm = java.util.Arrays.copyOf(nm, MAX_NAME); }
            this.frameBytes = frameBytes; block = frameBytes - HEAD; this.id = id & 0xffff;
            int total = 9 + nm.length + file.length;
            k = (total + block - 1) / block;
            if (k > MAX_K) throw new IllegalArgumentException("File too large for this mode");
            data = new byte[k * block];
            CRC32 crc = new CRC32(); crc.update(file);
            put32(data, 0, file.length); put32(data, 4, (int) crc.getValue()); data[8] = (byte) nm.length;
            System.arraycopy(nm, 0, data, 9, nm.length); System.arraycopy(file, 0, data, 9 + nm.length, file.length);
        }

        /** Frame number `index`: 0 to k-1 are the file itself, k and up are repair frames (up to 65535). */
        byte[] frame(int index) {
            byte[] f = new byte[frameBytes];
            f[0] = (byte) (id >> 8); f[1] = (byte) id; f[2] = (byte) (k >> 8); f[3] = (byte) k; f[4] = (byte) (index >> 8); f[5] = (byte) index;
            if (index < k) System.arraycopy(data, index * block, f, HEAD, block);
            else {
                byte[] c = new byte[k]; coeffs(id, index, c, k);
                for (int j = 0; j < k; j++) addMul(f, HEAD, data, j * block, block, c[j] & 255);
            }
            return f;
        }
    }

    static final class Rx {
        private final int frameBytes, block;
        private int id = -1, k, haveSrc; private byte[][] src; private boolean[] seenRepair;
        private final ArrayList<byte[]> repair = new ArrayList<>(); // whole frames
        private byte[] cand; // first frame of a possible new transmission
        String name; byte[] file; boolean done;

        Rx(int frameBytes) { this.frameBytes = frameBytes; block = frameBytes - HEAD; }

        int needed() { return id < 0 ? 0 : k; }
        int got() { return id < 0 ? 0 : Math.min(k, haveSrc + repair.size()); }

        /** Give it every good frame from the modem. Returns true once the file is complete and checked. */
        boolean add(byte[] f) {
            if (f.length != frameBytes) return done;
            int fid = (f[0] & 255) << 8 | f[1] & 255, fk = (f[2] & 255) << 8 | f[3] & 255, idx = (f[4] & 255) << 8 | f[5] & 255;
            if (fk < 1 || fk > MAX_K) return done;
            if (fid != id || fk != k) {
                // A different transmission. Believe it at once only when nothing is in progress; otherwise wait for a second frame.
                boolean second = cand != null && cand[0] == f[0] && cand[1] == f[1] && cand[2] == f[2] && cand[3] == f[3] && (cand[4] != f[4] || cand[5] != f[5]);
                if (id >= 0 && !second) { cand = f.clone(); return done; }
                byte[] first = second ? cand : null;
                start(fid, fk);
                if (first != null) store(first, (first[4] & 255) << 8 | first[5] & 255);
            }
            if (done) return true;
            store(f, idx);
            if (haveSrc + repair.size() >= k) solve();
            return done;
        }

        private void start(int fid, int fk) { id = fid; k = fk; haveSrc = 0; src = new byte[k][]; seenRepair = new boolean[65536]; repair.clear(); cand = null; done = false; name = null; file = null; }

        private void store(byte[] f, int idx) {
            if (idx < k) { if (src[idx] == null) { src[idx] = java.util.Arrays.copyOfRange(f, HEAD, frameBytes); haveSrc++; } }
            else if (!seenRepair[idx]) { seenRepair[idx] = true; repair.add(f.clone()); }
        }

        private void solve() {
            int m = k - haveSrc;
            if (m > 0) {
                int[] miss = new int[m]; for (int j = 0, n = 0; j < k; j++) if (src[j] == null) miss[n++] = j;
                int r = repair.size();
                byte[][] a = new byte[r][m], b = new byte[r][]; byte[] c = new byte[k];
                for (int i = 0; i < r; i++) {
                    byte[] f = repair.get(i);
                    coeffs(id, (f[4] & 255) << 8 | f[5] & 255, c, k);
                    b[i] = java.util.Arrays.copyOfRange(f, HEAD, frameBytes);
                    for (int j = 0; j < k; j++) if (src[j] != null) addMul(b[i], 0, src[j], 0, block, c[j] & 255); // take out the blocks we already have
                    for (int j = 0; j < m; j++) a[i][j] = c[miss[j]];
                }
                // Gauss-Jordan elimination
                for (int col = 0; col < m; col++) {
                    int p = col; while (p < r && a[p][col] == 0) p++;
                    if (p == r) return; // not enough independent frames yet, wait for one more
                    byte[] t = a[p]; a[p] = a[col]; a[col] = t; t = b[p]; b[p] = b[col]; b[col] = t;
                    int iv = inv(a[col][col] & 255);
                    if (iv != 1) { for (int j = col; j < m; j++) a[col][j] = (byte) mul(a[col][j] & 255, iv); for (int j = 0; j < block; j++) b[col][j] = (byte) mul(b[col][j] & 255, iv); }
                    for (int i = 0; i < r; i++) {
                        int v = a[i][col] & 255; if (i == col || v == 0) continue;
                        addMul(a[i], col, a[col], col, m - col, v); addMul(b[i], 0, b[col], 0, block, v);
                    }
                }
                for (int j = 0; j < m; j++) src[miss[j]] = b[j];
                haveSrc = k;
            }
            byte[] all = new byte[k * block];
            for (int j = 0; j < k; j++) System.arraycopy(src[j], 0, all, j * block, block);
            int len = get32(all, 0), nl = all[8] & 255;
            boolean ok = len >= 0 && nl <= MAX_NAME && 9L + nl + len <= all.length;
            if (ok) { CRC32 crc = new CRC32(); crc.update(all, 9 + nl, len); ok = (int) crc.getValue() == get32(all, 4); }
            if (!ok) { id = -1; return; } // a wrong frame slipped through: start again, a repeating sender fills it back in
            name = new String(all, 9, nl, StandardCharsets.UTF_8); file = java.util.Arrays.copyOfRange(all, 9 + nl, 9 + nl + len);
            done = true; src = null; repair.clear();
        }
    }

    private static void put32(byte[] b, int o, int v) { b[o] = (byte) (v >>> 24); b[o + 1] = (byte) (v >>> 16); b[o + 2] = (byte) (v >>> 8); b[o + 3] = (byte) v; }
    private static int get32(byte[] b, int o) { return (b[o] & 255) << 24 | (b[o + 1] & 255) << 16 | (b[o + 2] & 255) << 8 | b[o + 3] & 255; }
}
