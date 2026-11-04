package pl.dyktafon;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_PERM = 1, REQ_IMPORT = 2, REQ_COMPRESS = 3;
    private volatile float compProgress = -1; private volatile boolean compCancel;
    private String compMode; private boolean compDn, compVa;
    private static final int BG = Color.rgb(27, 36, 48);
    private WebView web;
    private String pendingMode; private boolean pendingDn, pendingVa; private int source = android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION, device = 0, treble = 1; private double floorGain = 0.18, liftDb = 6, gateDb = 12, compRatio = 3; private int rumbleHz = 180;
    private volatile Thread player;
    private volatile boolean playStop;
    private volatile long playFrame, playFrames, seekTo = -1;
    private volatile String playName;
    private volatile int playMs;
    private volatile String notice;

    private static volatile boolean idleSampling;
    private static File noiseFile(Context c) { return new File(c.getFilesDir(), "noise.bin"); }
    static double[] loadNoise(Context c) {
        File f = noiseFile(c); if (!f.exists()) return null;
        try (java.io.DataInputStream in = new java.io.DataInputStream(new FileInputStream(f))) {
            double[] p = new double[Denoiser.BINS]; for (int i = 0; i < p.length; i++) p[i] = in.readDouble(); return p;
        } catch (Exception e) { return null; }
    }
    static void saveNoise(Context c, double[] p) {
        if (p == null) return;
        try (java.io.DataOutputStream o = new java.io.DataOutputStream(new FileOutputStream(noiseFile(c)))) { for (double v : p) o.writeDouble(v); } catch (Exception ignored) {}
    }
    private void sampleIdle() {
        if (idleSampling) return;
        idleSampling = true;
        new Thread(() -> {
            android.media.AudioRecord rec = null; android.media.AudioDeviceInfo target = null;
            try {
                target = device != 0 ? RecService.route(this, device) : null;
                int min = android.media.AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                rec = new android.media.AudioRecord(target != null && RecService.isBluetooth(target) && RecService.callRouted ? android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION : source, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 16000));
                if (rec.getState() != android.media.AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("the microphone did not start");
                if (target != null) rec.setPreferredDevice(target);
                rec.startRecording();
                Denoiser d = new Denoiser(320, null); d.reduce = false;
                short[] pcm = new short[320]; double[] x = new double[320]; double hx = 0, hy = 0;
                for (int skip = 0; skip < 8; skip++) rec.read(pcm, 0, 320); // let the microphone settle
                d.startSample(2);
                while (d.sampling()) {
                    int got = 0; while (got < 320) { int r = rec.read(pcm, got, 320 - got); if (r < 0) throw new IllegalStateException("read error " + r); got += r; }
                    for (int i = 0; i < 320; i++) { double v = pcm[i]; hy = 0.92 * (hy + v - hx); hx = v; x[i] = hy; }
                    d.process(x, 320);
                }
                saveNoise(this, d.profile());
                notice = "Noise sample saved";
            } catch (Exception e) { notice = "Noise sample failed: " + e.getMessage(); }
            finally { if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} rec.release(); } try { RecService.unroute(this, target); } catch (Exception ignored) {} idleSampling = false; }
        }, "sample").start();
    }

    static String micName(android.media.AudioDeviceInfo d) {
        int t = d.getType(); String kind;
        if (t == android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC) kind = "Phone microphone";
        else if (t == android.media.AudioDeviceInfo.TYPE_USB_DEVICE || t == android.media.AudioDeviceInfo.TYPE_USB_HEADSET) kind = "USB";
        else if (t == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET) kind = "Wired headset";
        else if (RecService.isBluetooth(d)) kind = "Bluetooth";
        else return null;
        String addr = d.getAddress() == null ? "" : d.getAddress();
        CharSequence pn = d.getProductName();
        String extra = RecService.isBluetooth(d) || addr.isEmpty() ? (t == android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC ? "" : String.valueOf(pn == null ? "" : pn)) : addr;
        return kind + (extra.isEmpty() ? "" : " (" + extra + ")") + " #" + d.getId();
    }

    static File dir(Context c) { File d = new File(c.getExternalFilesDir(null), "recordings"); d.mkdirs(); return d; }
    private static String jq(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    private File named(String name) { return new File(dir(this), new File(name).getName()); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        web = new WebView(this);
        web.setBackgroundColor(BG);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setBlockNetworkLoads(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        // A WebView ignores its own padding, so the system-bar insets go on a parent layout.
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((View v, WindowInsets i) -> {
                android.graphics.Insets p = i.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime() | WindowInsets.Type.displayCutout());
                v.setPadding(p.left, p.top, p.right, p.bottom);
                return WindowInsets.CONSUMED;
            });
        } else root.setFitsSystemWindows(true);
        setContentView(root);
        web.loadUrl("file:///android_asset/index.html");
    }

    private void startRecording(String modeName, boolean dn, boolean va) {
        int mode = C2.modeOf(modeName);
        if (mode < 0) return;
        stopPlayback();
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date());
        Intent i = new Intent(this, RecService.class).putExtra("denoise", dn).putExtra("vad", va).putExtra("source", source).putExtra("device", device).putExtra("treble", treble).putExtra("floor", floorGain).putExtra("lift", liftDb).putExtra("rumble", rumbleHz).putExtra("gate", gateDb).putExtra("comp", compRatio).putExtra("mode", mode).putExtra("path", new File(dir(this), stamp + "_" + modeName + ".c2").getPath());
        try { startForegroundService(i); } catch (Exception e) { notice = "Can't start recording: " + e.getMessage(); }
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code != REQ_PERM) return;
        for (int k = 0; k < perms.length; k++) {
            if (Manifest.permission.RECORD_AUDIO.equals(perms[k]) && res[k] != PackageManager.PERMISSION_GRANTED) {
                notice = "Recording needs the microphone. Allow it in Android Settings > Apps > Codec2 Recorder > Permissions.";
                pendingMode = null; return;
            }
        }
        if (pendingMode != null) { startRecording(pendingMode, pendingDn, pendingVa); pendingMode = null; }
    }

    private void stopPlayback() {
        playStop = true; Thread t = player;
        if (t != null) { try { t.join(1000); } catch (InterruptedException ignored) {} }
        player = null;
    }

    private void play(String name, double fraction) {
        stopPlayback();
        File f = named(name);
        playStop = false; playName = name; playFrame = 0;
        player = new Thread(() -> {
            long h = 0; AudioTrack tr = null;
            try (RandomAccessFile in = new RandomAccessFile(f, "r")) {
                byte[] hd = new byte[C2.HEADER]; in.readFully(hd);
                int mode = C2.modeFromHeader(hd);
                if (mode < 0) throw new IllegalStateException("this isn't a Codec 2 file");
                h = C2.create(mode);
                int n = C2.samples(h), nb = C2.bytes(h);
                playMs = n / 8; playFrames = (f.length() - C2.HEADER) / nb;
                long start = (long) (fraction * playFrames);
                in.seek(C2.HEADER + start * nb); playFrame = start;
                int min = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                tr = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        .setAudioFormat(new AudioFormat.Builder().setSampleRate(8000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                        .setBufferSizeInBytes(Math.max(min, 8000)).setTransferMode(AudioTrack.MODE_STREAM).build();
                tr.play();
                byte[] bits = new byte[nb]; short[] pcm = new short[n]; Limiter lim = new Limiter(31000, 64);
                while (!playStop) {
                    if (seekTo >= 0) { in.seek(C2.HEADER + seekTo * nb); playFrame = seekTo; seekTo = -1; }
                    if (in.read(bits) != nb) break;
                    C2.decode(h, bits, pcm); boost(pcm, lim);
                    tr.write(pcm, 0, n);
                    playFrame++;
                }
                if (!playStop) { tr.stop(); }
            } catch (Exception e) {
                notice = "Can't play: " + e.getMessage();
            } finally {
                if (tr != null) { try { tr.pause(); tr.flush(); } catch (Exception ignored) {} tr.release(); }
                if (h != 0) C2.destroy(h);
                if (Thread.currentThread() == player) { player = null; playName = null; }
            }
        }, "play");
        player.start();
    }

    /** Decoded speech is quiet because recording keeps headroom for decoder peaks, so raise it here with a limiter that cannot clip. */
    private static final double PLAY_GAIN = 3.5;
    private static void boost(short[] pcm, Limiter l) { for (int i = 0; i < pcm.length; i++) pcm[i] = (short) Math.round(l.process(pcm[i] * PLAY_GAIN)); }

    private static byte[] wavHeader(int dataBytes) {
        byte[] h = new byte[44]; java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(h).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()); b.putInt(36 + dataBytes); b.put("WAVEfmt ".getBytes()); b.putInt(16); b.putShort((short) 1); b.putShort((short) 1);
        b.putInt(8000); b.putInt(16000); b.putShort((short) 2); b.putShort((short) 16); b.put("data".getBytes()); b.putInt(dataBytes);
        return h;
    }

    private void toDownloads(String display, String mime, byte[] head, InputStream body) throws Exception {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Downloads.DISPLAY_NAME, display);
        v.put(MediaStore.Downloads.MIME_TYPE, mime);
        v.put(MediaStore.Downloads.RELATIVE_PATH, "Download/Codec2Recorder");
        ContentResolver r = getContentResolver();
        Uri u = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) throw new IllegalStateException("Android refused to create the file");
        try (OutputStream o = r.openOutputStream(u)) {
            if (head != null) o.write(head);
            byte[] buf = new byte[8192]; int k;
            while ((k = body.read(buf)) > 0) o.write(buf, 0, k);
        }
    }

    private String displayName(Uri u) {
        try (android.database.Cursor c = getContentResolver().query(u, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) return c.getString(0);
        } catch (Exception ignored) {}
        return "audio";
    }

    private void compress(Uri u, String modeName, boolean dn, boolean va) {
        compCancel = false; compProgress = 0;
        final int sens = RecService.sensitivity; final double hold = RecService.holdSeconds; final int manual = RecService.gainDbLive;
        new Thread(() -> {
            android.media.MediaExtractor ex = new android.media.MediaExtractor();
            android.media.MediaCodec dec = null; long h = 0; File out = null; boolean ok = false;
            try {
                ex.setDataSource(this, u, null);
                int track = -1; android.media.MediaFormat fmt = null;
                for (int i = 0; i < ex.getTrackCount(); i++) { android.media.MediaFormat f = ex.getTrackFormat(i); String m = f.getString(android.media.MediaFormat.KEY_MIME); if (m != null && m.startsWith("audio/")) { track = i; fmt = f; break; } }
                if (track < 0) throw new IllegalStateException("no audio found in this file");
                ex.selectTrack(track);
                final long durUs = fmt.containsKey(android.media.MediaFormat.KEY_DURATION) ? fmt.getLong(android.media.MediaFormat.KEY_DURATION) : 0;
                dec = android.media.MediaCodec.createDecoderByType(fmt.getString(android.media.MediaFormat.KEY_MIME));
                dec.configure(fmt, null, null, 0); dec.start();
                int mode = C2.modeOf(modeName);
                h = C2.create(mode); if (h == 0) throw new IllegalStateException("the codec did not start");
                final long hh = h; if (mode == 8) C2.eq(h, true);
                final int n = C2.samples(h), nb = C2.bytes(h);
                String base = displayName(u).replaceAll("\\.[^.]*$", "").replaceAll("[^\\p{L}\\p{N} _.-]", "_");
                out = new File(dir(this), base + "_" + modeName + ".c2");
                for (int k = 2; out.exists(); k++) out = new File(dir(this), base + "_" + modeName + "_" + k + ".c2");
                final java.io.BufferedOutputStream os = new java.io.BufferedOutputStream(new FileOutputStream(out));
                final VoiceChain chain = new VoiceChain(n, dn, floorGain, treble, loadNoise(this), rumbleHz, liftDb, gateDb, compRatio);
                final short[] pcm = new short[n]; final byte[] bits = new byte[nb]; final int[] fill = {0};
                try {
                    os.write(C2.header(mode));
                    final int preN = Math.max(1, 400 / (n / 8)); final byte[][] pre = new byte[preN][nb]; final int[] preS = {0, 0}; final boolean[] was = {false};
                    Resampler rs = null; int channels = 1; boolean isFloat = false;
                    android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
                    boolean inEnd = false, outEnd = false;
                    while (!outEnd && !compCancel) {
                        if (!inEnd) {
                            int ii = dec.dequeueInputBuffer(5000);
                            if (ii >= 0) {
                                int sz = ex.readSampleData(dec.getInputBuffer(ii), 0);
                                if (sz < 0) { dec.queueInputBuffer(ii, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM); inEnd = true; }
                                else { dec.queueInputBuffer(ii, 0, sz, ex.getSampleTime(), 0); if (durUs > 0) compProgress = Math.min(0.99f, ex.getSampleTime() / (float) durUs); ex.advance(); }
                            }
                        }
                        int oi = dec.dequeueOutputBuffer(info, 5000);
                        if (oi == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) { rs = null; }
                        if (oi >= 0) {
                            if (rs == null) {
                                android.media.MediaFormat of = dec.getOutputFormat();
                                channels = of.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT);
                                isFloat = of.containsKey(android.media.MediaFormat.KEY_PCM_ENCODING) && of.getInteger(android.media.MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT;
                                rs = new Resampler(of.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE), v -> {
                                    pcm[fill[0]++] = (short) Math.max(-32767, Math.min(32767, v));
                                    if (fill[0] == n) {
                                        fill[0] = 0;
                                        boolean voice = chain.process(pcm, manual, sens, hold);
                                        C2.encode(hh, pcm, bits);
                                        boolean open = !va || voice;
                                        if (open) {
                                            if (!was[0]) { for (int c = preS[1]; c > 0; c--) os.write(pre[(preS[0] - c + preN) % preN]); preS[1] = 0; }
                                            os.write(bits);
                                        } else { System.arraycopy(bits, 0, pre[preS[0]], 0, nb); preS[0] = (preS[0] + 1) % preN; if (preS[1] < preN) preS[1]++; }
                                        was[0] = open;
                                    }
                                });
                            }
                            java.nio.ByteBuffer ob = dec.getOutputBuffer(oi).order(java.nio.ByteOrder.nativeOrder());
                            ob.position(info.offset); ob.limit(info.offset + info.size);
                            while (ob.remaining() >= channels * (isFloat ? 4 : 2)) {
                                double m = 0;
                                for (int c = 0; c < channels; c++) m += isFloat ? ob.getFloat() * 32767.0 : ob.getShort();
                                rs.put(m / channels);
                            }
                            dec.releaseOutputBuffer(oi, false);
                            if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outEnd = true;
                        }
                    }
                    ok = !compCancel;
                } finally { os.close(); }
                if (ok) notice = "Compressed to " + out.getName() + " (" + (out.length() / 1024) + " kB)"; else notice = "Compression cancelled";
            } catch (Exception e) {
                notice = "Can't compress this file: " + e.getMessage();
            } finally {
                if (!ok && out != null) out.delete();
                try { if (dec != null) { dec.stop(); dec.release(); } } catch (Exception ignored) {}
                ex.release(); if (h != 0) C2.destroy(h);
                compProgress = -1;
            }
        }, "compress").start();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_COMPRESS) { if (res == RESULT_OK && data != null && data.getData() != null) compress(data.getData(), compMode, compDn, compVa); return; }
        if (req != REQ_IMPORT || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        new Thread(() -> {
            try (InputStream in = new BufferedInputStream(getContentResolver().openInputStream(u))) {
                byte[] h = new byte[C2.HEADER]; int got = 0, k;
                while (got < h.length && (k = in.read(h, got, h.length - got)) > 0) got += k;
                if (got < h.length || C2.modeFromHeader(h) < 0) { notice = "This isn't a .c2 file with a Codec 2 header."; return; }
                String nm = "import_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date()) + "_" + C2.nameOf(C2.modeFromHeader(h)) + ".c2";
                try (FileOutputStream o = new FileOutputStream(new File(dir(this), nm))) {
                    o.write(h); byte[] buf = new byte[8192];
                    while ((k = in.read(buf)) > 0) o.write(buf, 0, k);
                }
                notice = "Imported " + nm;
            } catch (Exception e) { notice = "Import failed: " + e.getMessage(); }
        }).start();
    }

    class Bridge {
        @JavascriptInterface public String status() {
            String n = notice; notice = null;
            String err = RecService.error; RecService.error = null;
            StringBuilder s = new StringBuilder("{\"rec\":").append(RecService.running).append(",\"paused\":").append(RecService.paused).append(",\"waiting\":").append(RecService.waiting).append(",\"vad\":").append(RecService.vadMeter).append(",\"clip\":").append(System.currentTimeMillis() < RecService.clipUntil).append(",\"comp\":").append(compProgress).append(",\"gain\":").append(RecService.gainNow).append(",\"routed\":").append(jq(RecService.routed == null ? "" : RecService.routed))
                    .append(",\"level\":").append(RecService.level).append(",\"secs\":").append(RecService.frames * RecService.frameMs / 1000.0)
                    .append(",\"bytes\":").append(RecService.bytesWritten)
                    .append(",\"play\":").append(playName == null ? "null" : jq(playName))
                    .append(",\"pos\":").append(playFrame * playMs / 1000.0).append(",\"dur\":").append(playFrames * playMs / 1000.0);
            String rn = RecService.notice; RecService.notice = null;
            String msg = err != null ? err : (rn != null ? rn : n);
            s.append(",\"sampling\":").append(idleSampling || RecService.sampling || RecService.sampleReq).append(",\"hasNoise\":").append(noiseFile(MainActivity.this).exists());
            s.append(",\"msg\":").append(msg == null ? "null" : jq(msg)).append("}");
            return s.toString();
        }
        @JavascriptInterface public String list() {
            File[] fs = dir(MainActivity.this).listFiles();
            if (fs == null) return "[]";
            Arrays.sort(fs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            StringBuilder s = new StringBuilder("[");
            for (File f : fs) {
                if (RecService.running && f.getPath().equals(RecService.file)) continue;
                try {
                    int m = C2.readMode(f); if (m < 0) continue;
                    long h = C2.create(m); int n = C2.samples(h), nb = C2.bytes(h); C2.destroy(h);
                    double secs = (f.length() - C2.HEADER) / (double) nb * n / 8000.0;
                    if (s.length() > 1) s.append(',');
                    s.append("{\"name\":").append(jq(f.getName())).append(",\"mode\":").append(jq(C2.nameOf(m))).append(",\"secs\":").append(secs)
                            .append(",\"bytes\":").append(f.length()).append(",\"time\":").append(f.lastModified()).append('}');
                } catch (Exception ignored) {}
            }
            return s.append(']').toString();
        }
        @JavascriptInterface public void startRec(String mode, boolean dn, boolean va) {
            runOnUiThread(() -> {
                boolean mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                boolean nt = (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && (Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
                if (mic && (nt || getSharedPreferences("p", 0).getBoolean("asked2", false))) { startRecording(mode, dn, va); return; }
                pendingMode = mode; pendingDn = dn; pendingVa = va;
                getSharedPreferences("p", 0).edit().putBoolean("asked2", true).apply();
                requestPermissions(Build.VERSION.SDK_INT >= 33 ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.BLUETOOTH_CONNECT} : Build.VERSION.SDK_INT >= 31 ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH_CONNECT} : new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERM);
            });
        }
        @JavascriptInterface public String microphones() {
            StringBuilder s = new StringBuilder("[");
            for (android.media.AudioDeviceInfo d : getSystemService(AudioManager.class).getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                String nm = micName(d); if (nm == null) continue;
                if (s.length() > 1) s.append(',');
                s.append("{\"id\":").append(d.getId()).append(",\"name\":").append(jq(nm)).append('}');
            }
            return s.append(']').toString();
        }
        @JavascriptInterface public void audioConfig(int src, int dev, int trb, double fl, int rum, double lift, double gt, double cr) { gateDb = gt; compRatio = cr; source = src; device = dev; treble = trb; floorGain = fl; rumbleHz = rum; liftDb = lift; }
        @JavascriptInterface public void recLevel(int db) { RecService.gainDbLive = db; }
        @JavascriptInterface public void hold(double s) { RecService.holdSeconds = s; }
        @JavascriptInterface public void sensitivity(int v) { RecService.sensitivity = v; }
        @JavascriptInterface public void compressFile(String mode, boolean dn, boolean va) {
            runOnUiThread(() -> {
                if (compProgress >= 0) return;
                compMode = mode; compDn = dn; compVa = va;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
                startActivityForResult(i, REQ_COMPRESS);
            });
        }
        @JavascriptInterface public void cancelCompress() { compCancel = true; }
        @JavascriptInterface public void sampleNoise() {
            runOnUiThread(() -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERM); return; }
                stopPlayback();
                if (RecService.running) RecService.sampleReq = true; else sampleIdle();
            });
        }
        @JavascriptInterface public void clearNoise() { noiseFile(MainActivity.this).delete(); notice = "Noise sample cleared"; }
        @JavascriptInterface public void stopRec() { RecService.running = false; }
        @JavascriptInterface public void pauseRec(boolean p) { RecService.paused = p; }
        @JavascriptInterface public void play(String name) { runOnUiThread(() -> MainActivity.this.play(name, 0)); }
        @JavascriptInterface public void stopPlay() { runOnUiThread(MainActivity.this::stopPlayback); }
        @JavascriptInterface public void seek(double fraction) { seekTo = (long) (fraction * playFrames); }
        @JavascriptInterface public void remove(String name) { if (name.equals(playName)) stopPlay(); named(name).delete(); }
        @JavascriptInterface public boolean rename(String name, String to) {
            String clean = to.trim().replaceAll("[^\\p{L}\\p{N} _.-]", "_");
            if (clean.isEmpty()) return false;
            if (!clean.endsWith(".c2")) clean += ".c2";
            File dst = named(clean);
            return !dst.exists() && named(name).renameTo(dst);
        }
        @JavascriptInterface public void share(String name) {
            runOnUiThread(() -> {
                Uri u = new Uri.Builder().scheme("content").authority("pl.dyktafon.files").appendPath(new File(name).getName()).build();
                Intent i = new Intent(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Share recording"));
            });
        }
        @JavascriptInterface public void saveC2(String name) {
            new Thread(() -> {
                try (FileInputStream in = new FileInputStream(named(name))) { toDownloads(name, "application/octet-stream", null, in); notice = "Saved to Download/Codec2Recorder: " + name; }
                catch (Exception e) { notice = "Save failed: " + e.getMessage(); }
            }).start();
        }
        @JavascriptInterface public void saveWav(String name) {
            new Thread(() -> {
                long h = 0;
                try (FileInputStream in = new FileInputStream(named(name))) {
                    byte[] hd = new byte[C2.HEADER];
                    if (in.read(hd) != C2.HEADER || C2.modeFromHeader(hd) < 0) throw new IllegalStateException("invalid file");
                    h = C2.create(C2.modeFromHeader(hd));
                    int n = C2.samples(h), nb = C2.bytes(h);
                    byte[] bits = new byte[nb]; short[] pcm = new short[n];
                    ByteArrayOutputStream bo = new ByteArrayOutputStream(); Limiter lim = new Limiter(31000, 64);
                    while (in.read(bits) == nb) { C2.decode(h, bits, pcm); boost(pcm, lim); for (short v : pcm) { bo.write(v & 255); bo.write((v >> 8) & 255); } }
                    String wav = name.replaceAll("\\.c2$", "") + ".wav";
                    byte[] data = bo.toByteArray();
                    toDownloads(wav, "audio/wav", wavHeader(data.length), new java.io.ByteArrayInputStream(data));
                    notice = "Saved to Download/Codec2Recorder: " + wav;
                } catch (Exception e) { notice = "Save failed: " + e.getMessage(); }
                finally { if (h != 0) C2.destroy(h); }
            }).start();
        }
        @JavascriptInterface public void importFile() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                startActivityForResult(i, REQ_IMPORT);
            });
        }
    }

    @Override protected void onDestroy() { stopPlayback(); web.destroy(); super.onDestroy(); }
}
