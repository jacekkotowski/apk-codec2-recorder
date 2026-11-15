package pl.dyktafon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

/** Radio link by sound: the phone's speaker plays into the transmitter's microphone, the phone's microphone listens to the receiver's speaker.
 *  One job at a time: send a file, receive files, a test tone, or RADE voice messages (record, play, send, listen). Runs as a foreground service so it continues with the screen off. */
public class RadioService extends Service {
    static volatile boolean running, talk;
    static volatile String op = "", notice, error;
    static volatile int sent, frames, got, needed, framesOk, sync;
    static volatile float snr, level;
    static volatile long startedMs; static volatile double totalSecs, secs;
    private static final String CH = "radio";

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent in, int flags, int id) {
        if (in != null && "stop".equals(in.getAction())) { running = false; return START_NOT_STICKY; }
        if (in == null || running) return START_NOT_STICKY;
        final String what = in.getStringExtra("op");
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "Radio link", NotificationManager.IMPORTANCE_LOW));
        PendingIntent ps = PendingIntent.getService(this, 3, new Intent(this, RadioService.class).setAction("stop"), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open = PendingIntent.getActivity(this, 2, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CH).setContentTitle("Radio link").setContentText("Codec2 Recorder: " + what)
                .setSmallIcon(getResources().getIdentifier("ic", "drawable", getPackageName())).setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Stop", ps).build()).build();
        try { startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE); }
        catch (Exception e) { error = "Can't run the radio link in the background: " + e.getMessage(); stopSelf(); return START_NOT_STICKY; }
        op = what; error = null; running = true; talk = false; sent = frames = got = needed = framesOk = sync = 0; snr = 0; level = 0; startedMs = System.currentTimeMillis(); totalSecs = 0; secs = 0;
        final Intent args = in;
        DredLib.thread(() -> { // RADE and DRED need the larger native stack
            try {
                if ("send".equals(what)) send(args.getStringExtra("path"), args.getIntExtra("mode", 1), args.getIntExtra("extra", 50), args.getBooleanExtra("repeat", false));
                else if ("receive".equals(what)) receive(args.getIntExtra("mode", 1));
                else if ("rade_rec".equals(what)) radeRecord(args);
                else if ("rade_play".equals(what)) radePlay(args.getBooleanExtra("asRade", false));
                else if ("rade_send".equals(what)) radeSend();
                else if ("rade_listen".equals(what)) radeListen(args.getBooleanExtra("playAlong", false));
                else if ("tone".equals(what)) { AudioTrack t = track(); try { while (running) RadioLink.tone((s, c) -> t.write(s, 0, c), 1.0, 8000); } finally { drop(t); } }
            } catch (Throwable e) { error = "Radio link stopped: " + e.getMessage(); }
            finally { running = false; talk = false; op = ""; level = 0; sync = 0; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
        }, "radio").start();
        return START_NOT_STICKY;
    }

    /** Plain media output with no speech processing, so the modem tones reach the speaker unchanged. */
    private AudioTrack track() {
        int min = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(8000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(min, 8000)).setTransferMode(AudioTrack.MODE_STREAM).build();
        t.play(); return t;
    }
    private static void drop(AudioTrack t) { try { t.pause(); t.flush(); } catch (Exception ignored) {} t.release(); }

    /** Microphone with the least processing the phone offers: noise suppression and automatic gain damage modem tones. */
    private AudioRecord mic(int source) {
        int min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord r = new AudioRecord(source, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 16000));
        if (r.getState() != AudioRecord.STATE_INITIALIZED) { r.release(); throw new IllegalStateException("the microphone did not start"); }
        r.startRecording(); return r;
    }
    private int rawSource() {
        String u = getSystemService(AudioManager.class).getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED);
        return "true".equals(u) ? MediaRecorder.AudioSource.UNPROCESSED : MediaRecorder.AudioSource.VOICE_RECOGNITION;
    }
    private static void read(AudioRecord r, short[] b, int n) { int g = 0; while (g < n && running) { int k = r.read(b, g, n - g); if (k < 0) throw new IllegalStateException("read error " + k); g += k; } }
    private static float peak(short[] b, int n) { int p = 1; for (int i = 0; i < n; i++) p = Math.max(p, Math.abs((int) b[i])); return (float) Math.max(0, 1 + 20 * Math.log10(p / 32767.0) / 60); }

    private void send(String path, int mode, int extra, boolean repeat) throws Exception {
        File f = new File(path);
        RadioLink.Sender s = new RadioLink.Sender(mode, f.getName(), Files.readAllBytes(f.toPath()), extra);
        AudioTrack t = track();
        try {
            frames = repeat ? 0 : s.frames; totalSecs = repeat ? 0 : s.seconds();
            Thread watch = new Thread(() -> { while (running) { sent = s.sent; try { Thread.sleep(200); } catch (InterruptedException e) { return; } } });
            watch.start();
            s.run((b, n) -> t.write(b, 0, n), () -> !running, repeat);
            if (running) { t.stop(); Thread.sleep(600); notice = "Sent " + f.getName() + " (" + f.length() + " bytes)"; }
        } finally { drop(t); s.close(); }
    }

    private void receive(int mode) throws Exception {
        RadioLink.Receiver r = new RadioLink.Receiver(mode); AudioRecord rec = mic(rawSource());
        try {
            short[] b = new short[800];
            while (running) {
                read(rec, b, b.length); if (!running) break;
                level = peak(b, b.length);
                if (r.feed(b, b.length)) notice = save(r.name, r.file);
                got = r.got; needed = r.needed; framesOk = r.framesOk; sync = r.sync; snr = r.snr;
            }
        } finally { try { rec.stop(); } catch (Exception ignored) {} rec.release(); r.close(); }
    }

    /** A received recording joins the others in the app. Any other file goes to Download/Codec2Recorder, where the phone's Files app shows it. */
    private String save(String name, byte[] data) throws Exception {
        String clean = new File(name).getName().replaceAll("[^\\p{L}\\p{N} _.-]", "_"); if (clean.isEmpty()) clean = "received";
        boolean recording;
        try { Dred.readHeader(data); recording = true; } catch (Dred.BadFile e) { recording = false; }
        if (recording) {
            String base = clean.replaceAll("\\.[^.]*$", ""); File d = MainActivity.dir(this);
            File out = new File(d, "rx_" + base + ".dred");
            for (int k = 2; out.exists(); k++) out = new File(d, "rx_" + base + "_" + k + ".dred");
            try (FileOutputStream o = new FileOutputStream(out)) { o.write(data); }
            return "Received " + out.getName();
        }
        String mime = java.net.URLConnection.guessContentTypeFromName(clean);
        MainActivity.toDownloads(this, "rx_" + clean, mime == null ? "application/octet-stream" : mime, null, new java.io.ByteArrayInputStream(data));
        return "Received rx_" + clean + " (" + data.length + " bytes). Saved to Download/Codec2Recorder.";
    }

    // ---- RADE voice messages: record first, then send, so the phone's microphone and speaker are never on together ----

    /** The recorded message, 16 kHz speech, kept until the next one is recorded. */
    static File message(android.content.Context c) { return new File(c.getCacheDir(), "rade_message.raw"); }
    static double messageSecs(android.content.Context c) { return message(c).length() / 2.0 / Rade.SPEECH_FS; }
    static final int MAX_MESSAGE_S = 60;

    /** Records a message through the same voice chain and settings as a recording, up to MAX_MESSAGE_S. */
    private void radeRecord(Intent a) throws Exception {
        android.media.AudioDeviceInfo target = null; AudioRecord rec = null;
        File tmp = new File(getCacheDir(), "rade_message.tmp");
        try (java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(tmp))) {
            int device = a.getIntExtra("device", 0), fs = Rade.SPEECH_FS, n = Recorder.CHUNK;
            target = device != 0 ? RecService.route(this, device) : null;
            int src = target != null && RecService.isBluetooth(target) && RecService.callRouted ? MediaRecorder.AudioSource.VOICE_COMMUNICATION : a.getIntExtra("source", MediaRecorder.AudioSource.VOICE_RECOGNITION);
            int min = AudioRecord.getMinBufferSize(fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            rec = new AudioRecord(src, fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 2 * fs));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("the microphone did not start");
            if (target != null) rec.setPreferredDevice(target);
            VoiceChain chain = new VoiceChain(fs, VoiceChain.CEILING_DRED, n, a.getBooleanExtra("denoise", true), a.getDoubleExtra("floor", 0.18), a.getIntExtra("treble", 1), MainActivity.loadNoise(this, fs), a.getIntExtra("rumble", 180), a.getDoubleExtra("lift", 6), a.getDoubleExtra("gate", 12), a.getDoubleExtra("comp", 3));
            rec.startRecording();
            short[] pcm = new short[n]; byte[] b = new byte[2 * n]; long samples = 0;
            totalSecs = MAX_MESSAGE_S;
            while (running && samples < (long) MAX_MESSAGE_S * fs) {
                read(rec, pcm, n); if (!running) break;
                chain.process(pcm, RecService.gainDbLive, RecService.sensitivity, RecService.holdSeconds);
                level = (float) chain.level;
                for (int i = 0; i < n; i++) { b[2 * i] = (byte) pcm[i]; b[2 * i + 1] = (byte) (pcm[i] >> 8); }
                out.write(b); samples += n; secs = samples / (double) fs;
            }
        } finally {
            if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} rec.release(); }
            try { RecService.unroute(this, target); } catch (Exception ignored) {}
        }
        if (tmp.length() >= 2 * Rade.SPEECH_FS / 2) { tmp.renameTo(message(this)); notice = String.format(java.util.Locale.ROOT, "Message recorded, %.0f s. Play it, or send it.", messageSecs(this)); }
        else { tmp.delete(); notice = "Too short: nothing kept"; }
    }

    private short[] loadMessage() throws java.io.IOException {
        byte[] b = Files.readAllBytes(message(this).toPath()); short[] s = new short[b.length / 2];
        java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s); return s;
    }

    /** Speech output at 16 kHz, for playing the message and received voice. */
    private AudioTrack speechTrack() {
        int min = AudioTrack.getMinBufferSize(Rade.SPEECH_FS, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(Rade.SPEECH_FS).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(min, 2 * Rade.SPEECH_FS)).setTransferMode(AudioTrack.MODE_STREAM).build();
        t.play(); return t;
    }
    private static void write(AudioTrack t, short[] b, int n) { for (int o = 0; o < n && running; ) { int w = t.write(b, o, n - o); if (w < 0) throw new IllegalStateException("audio output error " + w); o += w; } }
    private static void finish(AudioTrack t, long samples) throws InterruptedException {
        t.stop(); long end = System.currentTimeMillis() + 3000;
        while (running && t.getPlaybackHeadPosition() < samples && System.currentTimeMillis() < end) Thread.sleep(30);
    }

    /** Plays the message as recorded, or asRade: sent and received inside the phone, so it sounds as a station would hear it on a clean channel. */
    private void radePlay(boolean asRade) throws Exception {
        short[] sp = loadMessage(); totalSecs = sp.length / (double) Rade.SPEECH_FS;
        Limiter lim = new Limiter(31000, 128, Rade.SPEECH_FS); AudioTrack t = speechTrack(); long played = 0;
        try {
            if (!asRade) {
                short[] b = new short[Recorder.CHUNK];
                for (int i = 0; i < sp.length && running; i += b.length) {
                    int n = Math.min(b.length, sp.length - i);
                    for (int k = 0; k < n; k++) b[k] = (short) Math.round(lim.process(sp[i + k] * 2.0)); // the same playback level as recordings
                    write(t, b, n); played += n; secs = played / (double) Rade.SPEECH_FS;
                }
            } else try (Rade r = new Rade()) {
                short[] m = r.send(sp, sp.length, 0), step = new short[Rade.STEP], out = new short[Rade.OUT_PER_STEP];
                for (int i = 0; i < m.length && running; i += Rade.STEP) {
                    int n = Math.min(Rade.STEP, m.length - i); System.arraycopy(m, i, step, 0, n);
                    int k = r.receive(step, n, out);
                    for (int j = 0; j < k; j++) out[j] = (short) Math.round(lim.process(out[j] * 2.0));
                    write(t, out, k); played += k; secs = played / (double) Rade.SPEECH_FS; sync = r.sync() ? 1 : 0;
                }
            }
            if (running) finish(t, played);
        } finally { drop(t); }
    }

    /** Sends the message: RADE modem sound from the speaker, nothing from the microphone. Half a second of coded silence first opens a VOX. */
    private void radeSend() throws Exception {
        short[] sp = loadMessage(); short[] m;
        try (Rade r = new Rade()) { m = r.send(sp, sp.length, 500); }
        totalSecs = m.length / (double) Rade.MODEM_FS;
        AudioTrack t = track();
        try {
            for (int i = 0; i < m.length && running; i += Rade.STEP) {
                int n = Math.min(Rade.STEP, m.length - i); short[] part = java.util.Arrays.copyOfRange(m, i, i + n);
                level = peak(part, n); write(t, part, n); secs = (i + n) / (double) Rade.MODEM_FS;
            }
            if (running) { finish(t, m.length); notice = String.format(java.util.Locale.ROOT, "Sent as RADE, %.0f s on air", totalSecs); }
        } finally { drop(t); }
    }

    /** Listens for RADE. Each received message (until the sender's end-of-over, or 3 s after the signal is lost) is saved to the recordings as DRED best.
     *  playAlong plays the voice as it arrives: use earphones, or the phone's speaker reaches its own microphone. */
    private void radeListen(boolean playAlong) throws Exception {
        AudioRecord rec = mic(rawSource()); AudioTrack t = playAlong ? speechTrack() : null; Limiter lim = new Limiter(31000, 128, Rade.SPEECH_FS);
        java.io.ByteArrayOutputStream heard = new java.io.ByteArrayOutputStream(); int lostSteps = 0; int saved = 0;
        try (Rade r = new Rade()) {
            short[] in = new short[Rade.STEP], out = new short[Rade.OUT_PER_STEP];
            while (running) {
                read(rec, in, in.length); if (!running) break;
                level = peak(in, in.length);
                int k = r.receive(in, in.length, out);
                boolean locked = r.sync(); sync = locked ? 1 : 0; if (locked) snr = r.snr();
                for (int j = 0; j < k; j++) { short v = out[j]; heard.write(v & 255); heard.write((v >> 8) & 255); }
                if (t != null && k > 0) { for (int j = 0; j < k; j++) out[j] = (short) Math.round(lim.process(out[j] * 2.0)); write(t, out, k); }
                secs = heard.size() / 2.0 / Rade.SPEECH_FS;
                lostSteps = locked ? 0 : lostSteps + 1;
                boolean over = r.endOfOver() > 0 || (lostSteps >= 30 && heard.size() > 0);
                if (over && heard.size() >= Rade.SPEECH_FS) { notice = saveHeard(heard.toByteArray()); saved++; heard.reset(); }
                else if (over) heard.reset();
            }
            if (heard.size() >= Rade.SPEECH_FS) { notice = saveHeard(heard.toByteArray()); saved++; }
        } finally {
            try { rec.stop(); } catch (Exception ignored) {} rec.release(); if (t != null) drop(t);
        }
        if (saved == 0 && notice == null) notice = "No RADE message was received";
    }

    /** A received message joins the recordings, coded with DRED at the finest level so it loses as little as possible again. */
    private String saveHeard(byte[] pcm) throws java.io.IOException {
        String stamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.ROOT).format(new java.util.Date());
        File out = new File(MainActivity.dir(this), "rx_RADE_" + stamp + "_DRED-best.dred");
        int q = Dred.qOf("best"); short[] f = new short[Dred.FRAME];
        try (java.io.BufferedOutputStream o = new java.io.BufferedOutputStream(new FileOutputStream(out)); DredLib.Coder c = new DredLib.Coder(q)) {
            o.write(Dred.header(q));
            for (int i = 0; i + 2 * Dred.FRAME <= pcm.length; i += 2 * Dred.FRAME) {
                for (int k = 0; k < Dred.FRAME; k++) f[k] = (short) ((pcm[i + 2 * k] & 255) | (pcm[i + 2 * k + 1] << 8));
                int n = c.frame(f); if (n >= 0) Dred.writeBlock(o, c.buf, n);
            }
            c.finish((b, n) -> Dred.writeBlock(o, b, n));
        }
        return String.format(java.util.Locale.ROOT, "Received a RADE message, %.0f s: saved as %s", pcm.length / 2.0 / Rade.SPEECH_FS, out.getName());
    }
}
