package pl.dyktafon;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The self-test on the phone (stage 0 of ROADMAP.md): runs SelfTest on a worker thread, puts the phone's own speaker and microphone
 * in the modem loop, and keeps one text report. The test itself is in SelfTest, which also runs on a computer.
 * One job at a time. The page polls state(); the report is shared as text and saved to Download/Codec2Recorder.
 */
final class SelfTestRun {
    private static final StringBuilder text = new StringBuilder();
    private static volatile boolean running, cancel, done;
    private static volatile String step = "";

    static synchronized String report() { return text.toString(); }
    private static synchronized void add(String s) { text.append(s).append('\n'); }
    static boolean running() { return running; }
    static void cancel() { cancel = true; }

    /** Status for the page. */
    static org.json.JSONObject state() throws org.json.JSONException {
        return new org.json.JSONObject().put("on", running).put("done", done).put("step", step).put("text", report());
    }

    private static final SelfTest.Log LOG = new SelfTest.Log() {
        @Override public void line(String s) { add(s); }
        @Override public boolean cancelled() { return cancel; }
    };

    private static void begin(String title) { cancel = false; done = false; running = true; step = title; }

    /** Full self-test: device, speed, modem loop. onDone runs on the worker thread when it ends. */
    static void runAll(Context c, Runnable onDone) {
        synchronized (SelfTestRun.class) { text.setLength(0); }
        begin("Starting");
        DredLib.thread(() -> {
            try {
                add("Self-test " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
                device(c);
                part("Speed: voice chain, DRED", () -> SelfTest.dredSpeed(LOG, 20));
                part("Speed: RADE", () -> SelfTest.radeSpeed(LOG, 10));
                part("Modem loop through the speaker and microphone", () -> modemLoop(c));
                add(""); add(cancel ? "Stopped before the end." : "End of report.");
                saveReport(c);
            } finally { running = false; done = true; step = ""; onDone.run(); }
        }, "selftest").start();
    }

    private interface Part { void run() throws Exception; }
    /** A part that fails reports why and the test goes on: one broken library must not hide the other results. */
    private static void part(String title, Part p) {
        if (cancel) return;
        step = title; add(""); add("== " + title + " ==");
        try { p.run(); } catch (Throwable e) { add("FAILED: " + e); }
    }

    /** 30 seconds of the microphone with no processing by the app, saved as a WAV file for tests on a computer. */
    static void clip(Context c, int source, Runnable onDone) {
        synchronized (SelfTestRun.class) { text.setLength(0); }
        begin("Test clip");
        DredLib.thread(() -> {
            AudioRecord rec = null;
            try {
                add("Test clip " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
                final int fs = Dred.FS, total = 30 * fs, n = 1600; short[] all = new short[total], b = new short[n];
                int min = AudioRecord.getMinBufferSize(fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                rec = new AudioRecord(source, fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 2 * fs));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("the microphone did not start");
                rec.startRecording();
                int got = 0;
                while (got < total && !cancel) {
                    int k = rec.read(b, 0, Math.min(n, total - got));
                    if (k < 0) throw new IllegalStateException("read error " + k);
                    System.arraycopy(b, 0, all, got, k); got += k; step = "Test clip: " + got / fs + " of 30 s";
                }
                String name = "testclip_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date()) + ".wav";
                final short[] heard = java.util.Arrays.copyOf(all, got);
                saveWav(c, name, heard, fs);
                add("Source " + sourceName(source) + ", no processing by the app.");
                add(SelfTest.clipSummary(heard, fs));
                add("Saved to Download/Codec2Recorder/" + name);
                saveReport(c);
            } catch (Throwable e) { add("FAILED: " + e); }
            finally { if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} rec.release(); } running = false; done = true; step = ""; onDone.run(); }
        }, "testclip").start();
    }

    // ---- parts ----

    private static void device(Context c) {
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            add("App " + c.getPackageName() + " " + pi.versionName + " (" + pi.getLongVersionCode() + ")");
        } catch (Exception e) { add("App version unknown: " + e); }
        add("Phone: " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        add("Processor types: " + String.join(", ", Build.SUPPORTED_ABIS) + "; " + Runtime.getRuntime().availableProcessors() + " cores; "
                + (Runtime.getRuntime().maxMemory() >> 20) + " MB for the app");
        AudioManager am = c.getSystemService(AudioManager.class);
        add("Media volume " + am.getStreamVolume(AudioManager.STREAM_MUSIC) + " of " + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                + " (about three quarters is right for the modem loop)");
        add("Audio: output " + am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) + " Hz, buffer " + am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
                + " frames, unprocessed microphone source " + ("true".equals(am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)) ? "yes" : "no"));
        StringBuilder o = new StringBuilder();
        for (AudioDeviceInfo d : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) o.append(o.length() > 0 ? ", " : "").append(typeName(d.getType()));
        StringBuilder m = new StringBuilder();
        for (AudioDeviceInfo d : am.getDevices(AudioManager.GET_DEVICES_INPUTS)) { String nm = MainActivity.micName(d); if (nm != null) m.append(m.length() > 0 ? ", " : "").append(nm); }
        add("Outputs: " + o); add("Microphones: " + m);
        add("Microphone source for the modem loop: " + sourceName(rawSource(c)));
    }

    private static String typeName(int t) {
        switch (t) {
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: return "speaker";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE: return "earpiece";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET: case AudioDeviceInfo.TYPE_WIRED_HEADPHONES: return "wired headset";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return "Bluetooth";
            case AudioDeviceInfo.TYPE_USB_DEVICE: case AudioDeviceInfo.TYPE_USB_HEADSET: return "USB";
            default: return "type " + t;
        }
    }
    private static String sourceName(int s) {
        return s == MediaRecorder.AudioSource.UNPROCESSED ? "UNPROCESSED (9)" : s == MediaRecorder.AudioSource.VOICE_RECOGNITION ? "VOICE_RECOGNITION (6)" : s == MediaRecorder.AudioSource.MIC ? "MIC (1)" : String.valueOf(s);
    }
    /** The microphone with the least processing the phone offers, as RadioService uses for the modem. */
    static int rawSource(Context c) {
        String u = c.getSystemService(AudioManager.class).getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED);
        return "true".equals(u) ? MediaRecorder.AudioSource.UNPROCESSED : MediaRecorder.AudioSource.VOICE_RECOGNITION;
    }

    private static void modemLoop(Context c) throws Exception {
        add("Keep the phone still on a table in a quiet room. The phone's speaker plays modem sound and its microphone listens.");
        java.util.ArrayList<short[]> heard = SelfTest.modemLoop(LOG, new SpeakerToMic(c));
        if (heard.isEmpty()) return;
        // Everything the microphone heard, one second of silence between runs, so it can be looked at on a computer.
        int len = 0; for (short[] h : heard) len += h.length + 8000;
        short[] all = new short[len]; int p = 0; for (short[] h : heard) { System.arraycopy(h, 0, all, p, h.length); p += h.length + 8000; }
        String name = "selftest_mic_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date()) + ".wav";
        saveWav(c, name, all, 8000);
        add("What the microphone heard in all runs (8 kHz, one second between runs): Download/Codec2Recorder/" + name);
    }

    /** The phone's own speaker to its own microphone. */
    private static final class SpeakerToMic implements SelfTest.Path {
        private final Context c; SpeakerToMic(Context c) { this.c = c; }
        @Override public SelfTest.Heard through(short[] tx, int n, double gain) throws Exception {
            final int lead = 8000, tail = 16000; short[] mic = new short[lead + n + tail];
            int min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord rec = new AudioRecord(rawSource(c), 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 16000));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) { rec.release(); throw new IllegalStateException("the microphone did not start"); }
            int tmin = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack t = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(8000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(Math.max(tmin, 8000)).setTransferMode(AudioTrack.MODE_STREAM).build();
            Thread player = null;
            try {
                rec.startRecording();
                int got = 0; short[] b = new short[800];
                while (got < mic.length && !cancel) {
                    if (got >= lead && player == null) {
                        t.play();
                        player = new Thread(() -> {
                            short[] w = new short[800];
                            for (int i = 0; i < n && !cancel; i += w.length) {
                                int k = Math.min(w.length, n - i);
                                for (int j = 0; j < k; j++) w[j] = (short) Math.max(-32767, Math.min(32767, Math.round(tx[i + j] * gain)));
                                int o = 0; while (o < k && !cancel) { int r = t.write(w, o, k - o); if (r < 0) return; o += r; }
                            }
                        }, "selftest-play");
                        player.start();
                    }
                    int k = rec.read(b, 0, Math.min(b.length, mic.length - got));
                    if (k < 0) throw new IllegalStateException("read error " + k);
                    System.arraycopy(b, 0, mic, got, k); got += k;
                }
                if (player != null) player.join(2000);
            } finally {
                try { rec.stop(); } catch (Exception ignored) {} rec.release();
                try { t.pause(); t.flush(); } catch (Exception ignored) {} t.release();
            }
            return new SelfTest.Heard(mic, lead);
        }
    }

    // ---- saving ----

    private static void saveWav(Context c, String name, short[] pcm, int fs) throws Exception {
        MainActivity.toDownloads(c, name, "audio/wav", o -> { o.write(MainActivity.wavHeader(pcm.length * 2, fs)); o.write(SelfTest.shortsToBytes(pcm, pcm.length)); });
    }
    private static void saveReport(Context c) {
        try {
            String name = "selftest_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date()) + ".txt";
            byte[] b = report().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            MainActivity.toDownloads(c, name, "text/plain", null, new java.io.ByteArrayInputStream(b));
            add("Report saved: Download/Codec2Recorder/" + name);
        } catch (Exception e) { add("The report could not be saved: " + e.getMessage()); }
    }
}
