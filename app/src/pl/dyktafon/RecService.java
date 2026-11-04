package pl.dyktafon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/** Records in a foreground service, so recording continues with the screen off. */
public class RecService extends Service {
    static volatile boolean running, paused, waiting, sampleReq, sampling;
    static volatile String notice;
    static volatile int sensitivity = 5;
    static volatile float vadMeter, gainNow;
    static volatile long clipUntil;
    static volatile int gainDbLive = -100; // -100 means automatic
    static volatile double holdSeconds = 1;
    static volatile String routed = "";
    static volatile float level;
    static volatile long frames, bytesWritten;
    static volatile int frameMs;
    static volatile String file, error;
    private static final String CH = "rec";
    private Thread thread;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent in, int flags, int id) {
        if (in != null && "stop".equals(in.getAction())) { running = false; return START_NOT_STICKY; }
        if (in == null || running) return START_NOT_STICKY;
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "Recording", NotificationManager.IMPORTANCE_LOW));
        Intent stop = new Intent(this, RecService.class).setAction("stop");
        PendingIntent ps = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open = PendingIntent.getActivity(this, 2, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CH).setContentTitle("Recording").setContentText("Codec2 Recorder is recording")
                .setSmallIcon(R_ic()).setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Stop", ps).build()).build();
        try {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } catch (Exception e) { error = "Can't record in the background: " + e.getMessage(); stopSelf(); return START_NOT_STICKY; }
        final int mode = in.getIntExtra("mode", 8); final boolean dn = in.getBooleanExtra("denoise", false), va = in.getBooleanExtra("vad", false); final int source = in.getIntExtra("source", MediaRecorder.AudioSource.VOICE_RECOGNITION), device = in.getIntExtra("device", 0), treble = in.getIntExtra("treble", 1); final double strength = in.getDoubleExtra("floor", 0.18), lift = in.getDoubleExtra("lift", 6), gate = in.getDoubleExtra("gate", 12), comp = in.getDoubleExtra("comp", 3); final int rumble = in.getIntExtra("rumble", 180);
        final String path = in.getStringExtra("path");
        file = path; error = null; running = true; paused = false; frames = 0; bytesWritten = 0; level = 0;
        waiting = false; thread = new Thread(() -> loop(mode, path, dn, va, source, device, treble, strength, rumble, lift, gate, comp), "rec");
        thread.start();
        return START_NOT_STICKY;
    }
    private int R_ic() { return getResources().getIdentifier("ic", "drawable", getPackageName()); }

    /** Bluetooth mics use Android's call audio path. Returns the device to record from, or null to use the normal route. */
    static android.media.AudioDeviceInfo route(android.content.Context c, int deviceId) throws InterruptedException {
        android.media.AudioManager am = c.getSystemService(android.media.AudioManager.class);
        for (android.media.AudioDeviceInfo d : am.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS)) {
            if (d.getId() != deviceId) continue;
            if (!isBluetooth(d)) return d;
            am.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
            boolean ok = false;
            try {
                if (Build.VERSION.SDK_INT >= 31) {
                    // Android routes calls by the headset's output side, not the microphone device itself.
                    for (android.media.AudioDeviceInfo out : am.getAvailableCommunicationDevices())
                        if (isBluetooth(out) && (ok = am.setCommunicationDevice(out))) break;
                    for (int w = 0; ok && w < 30; w++) {
                        android.media.AudioDeviceInfo cur = am.getCommunicationDevice();
                        if (cur != null && isBluetooth(cur)) break;
                        Thread.sleep(100);
                    }
                } else { am.startBluetoothSco(); am.setBluetoothScoOn(true); ok = true; }
            } catch (RuntimeException e) { ok = false; }
            if (!ok) am.setMode(android.media.AudioManager.MODE_NORMAL);
            callRouted = ok;
            Thread.sleep(ok ? 800 : 0); // the headset needs a moment to open its microphone link
            return d;
        }
        return null;
    }
    static volatile boolean callRouted;
    static void unroute(android.content.Context c, android.media.AudioDeviceInfo d) {
        if (d == null || !isBluetooth(d) || !callRouted) return;
        callRouted = false;
        android.media.AudioManager am = c.getSystemService(android.media.AudioManager.class);
        if (Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice(); else { am.setBluetoothScoOn(false); am.stopBluetoothSco(); }
        am.setMode(android.media.AudioManager.MODE_NORMAL);
    }
    static boolean isBluetooth(android.media.AudioDeviceInfo d) {
        return d.getType() == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (Build.VERSION.SDK_INT >= 31 && d.getType() == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET);
    }

    private void loop(int mode, String path, boolean denoise, boolean voiceOnly, int source, int device, int treble, double strength, int rumble, double lift, double gate, double comp) {
        long h = 0; AudioRecord rec = null; android.media.AudioDeviceInfo target = null;
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(path)))) {
            h = C2.create(mode);
            if (h == 0) throw new IllegalStateException("the codec did not start");
            int n = C2.samples(h), nb = C2.bytes(h);
            frameMs = n / 8;
            int min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            target = device != 0 ? route(this, device) : null;
            int src = target != null && isBluetooth(target) && callRouted ? MediaRecorder.AudioSource.VOICE_COMMUNICATION : source; // if the call route failed, use the plain route
            rec = new AudioRecord(src, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 16000));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("the microphone did not start");
            if (target != null) rec.setPreferredDevice(target);
            if (mode == 8) C2.eq(h, true); // Codec 2's own equaliser for 700C adapts to the microphone's response
            out.write(C2.header(mode)); bytesWritten = C2.HEADER;
            rec.startRecording();
            android.media.AudioDeviceInfo rd = rec.getRoutedDevice();
            routed = rd == null ? "" : MainActivity.micName(rd);
            short[] pcm = new short[n]; byte[] bits = new byte[nb];
            VoiceChain chain = new VoiceChain(n, denoise, strength, treble, MainActivity.loadNoise(this), rumble, lift, gate, comp);
            // Keep about 400 ms of encoded audio so the start of a word is not cut by voice activation.
            int preN = Math.max(1, 400 / frameMs), preW = 0, preC = 0; byte[][] pre = new byte[preN][nb];
            int sinceFlush = 0; boolean wasOpen = false; long silentFrames = 0; boolean warned = false, clipWarned = false;
            while (running) {
                int got = 0;
                while (got < n && running) { int r = rec.read(pcm, got, n - got); if (r < 0) throw new IllegalStateException("read error " + r); got += r; }
                if (got < n) break;
                if (paused) { level = 0; waiting = false; continue; }
                boolean allZero = true; for (short v : pcm) if (Math.abs(v) > 1) { allZero = false; break; }
                if (allZero) { if (!warned && ++silentFrames * frameMs > 3000) { warned = true; notice = "No sound from this microphone. Try another one, or switch the headset off and on."; } } else silentFrames = 0;
                if (sampleReq) { sampleReq = false; sampling = true; chain.den.startSample(2); }
                boolean voice = chain.process(pcm, gainDbLive, sensitivity, holdSeconds);
                if (sampling && !chain.den.sampling()) { sampling = false; MainActivity.saveNoise(this, chain.den.profile()); notice = "Noise sample saved"; }
                if (chain.inputClipped && !clipWarned) { clipWarned = true; notice = "The microphone itself is overloading before the app receives the sound. Move it further away or try another audio source."; }
                vadMeter = (float) chain.meter; gainNow = (float) chain.currentGain; level = (float) chain.level; if (chain.frameClipped) clipUntil = System.currentTimeMillis() + 1000;
                C2.encode(h, pcm, bits);
                boolean open = !voiceOnly || voice;
                waiting = voiceOnly && !voice;
                if (open) {
                    if (!wasOpen) { for (int k = preC; k > 0; k--) { out.write(pre[(preW - k + preN) % preN]); frames++; bytesWritten += nb; } preC = 0; }
                    out.write(bits); frames++; bytesWritten += nb;
                    if (++sinceFlush * frameMs >= 1000) { out.flush(); sinceFlush = 0; }
                } else {
                    System.arraycopy(bits, 0, pre[preW], 0, nb); preW = (preW + 1) % preN; if (preC < preN) preC++;
                }
                wasOpen = open;
            }
            out.flush();
        } catch (Exception e) {
            error = "Recording stopped: " + e.getMessage();
        } finally {
            if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} rec.release(); }
            try { unroute(this, target); } catch (Exception ignored) {}
            if (h != 0) C2.destroy(h);
            running = false; level = 0; waiting = false; sampling = false;
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
        }
    }
}
