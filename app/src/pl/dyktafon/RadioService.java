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
 *  One job at a time: send a file, receive files, live voice, or a test tone. Runs as a foreground service so it continues with the screen off. */
public class RadioService extends Service {
    static volatile boolean running, talk;
    static volatile String op = "", notice, error;
    static volatile int sent, frames, got, needed, framesOk, sync;
    static volatile float snr, level;
    static volatile long startedMs; static volatile double totalSecs;
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
        op = what; error = null; running = true; talk = false; sent = frames = got = needed = framesOk = sync = 0; snr = 0; level = 0; startedMs = System.currentTimeMillis(); totalSecs = 0;
        final Intent args = in;
        new Thread(() -> {
            try {
                if ("send".equals(what)) send(args.getStringExtra("path"), args.getIntExtra("mode", 1), args.getIntExtra("extra", 50), args.getBooleanExtra("repeat", false));
                else if ("receive".equals(what)) receive(args.getIntExtra("mode", 1));
                else if ("live".equals(what)) live(args);
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
        if (data.length >= C2.HEADER && C2.modeFromHeader(data) >= 0) {
            String base = clean.replaceAll("\\.[^.]*$", ""); File d = MainActivity.dir(this);
            File out = new File(d, "rx_" + base + ".c2");
            for (int k = 2; out.exists(); k++) out = new File(d, "rx_" + base + "_" + k + ".c2");
            try (FileOutputStream o = new FileOutputStream(out)) { o.write(data); }
            return "Received " + out.getName();
        }
        String mime = java.net.URLConnection.guessContentTypeFromName(clean);
        MainActivity.toDownloads(this, "rx_" + clean, mime == null ? "application/octet-stream" : mime, null, new java.io.ByteArrayInputStream(data));
        return "Received rx_" + clean + " (" + data.length + " bytes). Saved to Download/Codec2Recorder.";
    }

    /** Live voice, one direction at a time. Listening decodes the radio's sound; talking sends the microphone through the voice chain and the modem. */
    private void live(Intent a) throws Exception {
        long h = Radio.open(a.getIntExtra("mode", 10), 1);
        if (h == 0) throw new IllegalStateException("the modem did not start");
        final int ns = Radio.speechN(h), nm = Radio.modemN(h), cf = 320;
        final int voiceSource = a.getBooleanExtra("echo", false) ? MediaRecorder.AudioSource.VOICE_COMMUNICATION : a.getIntExtra("source", MediaRecorder.AudioSource.VOICE_RECOGNITION);
        VoiceChain chain = new VoiceChain(cf, a.getBooleanExtra("denoise", true), a.getDoubleExtra("floor", 0.18), a.getIntExtra("treble", 1), MainActivity.loadNoise(this), a.getIntExtra("rumble", 180), a.getDoubleExtra("lift", 6), a.getDoubleExtra("gate", 12), a.getDoubleExtra("comp", 3));
        AudioTrack t = track(); AudioRecord rec = null; android.media.audiofx.AcousticEchoCanceler aec = null;
        short[] sp = new short[ns], mod = new short[nm], part = new short[cf], in = new short[640], out = new short[8 * ns];
        Limiter lim = new Limiter(31000, 64);
        try {
            boolean talking = !talk; // forces the microphone to open on the first pass
            while (running) {
                if (talking != talk) {
                    talking = talk;
                    if (aec != null) { aec.release(); aec = null; }
                    if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} rec.release(); }
                    rec = mic(talking ? voiceSource : rawSource());
                    if (talking && voiceSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION && android.media.audiofx.AcousticEchoCanceler.isAvailable()) { aec = android.media.audiofx.AcousticEchoCanceler.create(rec.getAudioSessionId()); if (aec != null) aec.setEnabled(true); }
                    sync = 0;
                }
                if (talking) {
                    read(rec, sp, ns); if (!running) break;
                    for (int p = 0; p < ns; p += cf) { System.arraycopy(sp, p, part, 0, cf); chain.process(part, RecService.gainDbLive, 5, 1); System.arraycopy(part, 0, sp, p, cf); }
                    level = (float) chain.level;
                    Radio.txVoice(h, sp, mod); t.write(mod, 0, nm);
                } else {
                    read(rec, in, in.length); if (!running) break;
                    level = peak(in, in.length);
                    int m = Radio.rxVoice(h, in, in.length, out);
                    sync = Radio.sync(h); snr = Radio.snr(h);
                    for (int i = 0; i < m; i++) out[i] = (short) Math.round(lim.process(out[i] * 2.0));
                    if (m > 0) t.write(out, 0, m);
                }
            }
        } finally {
            if (aec != null) aec.release();
            if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} rec.release(); }
            drop(t); Radio.close(h);
        }
    }
}
