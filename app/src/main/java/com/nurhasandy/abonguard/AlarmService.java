package com.nurhasandy.abonguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Service latar depan: bunyi alarm + getar + notifikasi layar penuh. */
public class AlarmService extends Service {
    public static final String ACT_SILENCE = "com.nurhasandy.abonguard.SILENCE";
    public static final String ACT_RESUME = "com.nurhasandy.abonguard.RESUME";
    private static final String CHANNEL = "absen_alarm";

    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> Scheduler.release(AlarmService.this, false);

    private static final AudioAttributes ALARM_ATTRS = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

    public static Intent lockIntent(Context c) {
        return new Intent(c, LockActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }

        if (!Prefs.locked(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent == null ? null : intent.getAction();
        if (ACT_SILENCE.equals(action)) {
            stopSound();
        } else {
            startSound();
            if (action == null) {
                acquireWake();
                try { startActivity(lockIntent(this)); } catch (Exception ignored) {}
            }
        }

        long remaining = Prefs.MAX_LOCK_MS - (System.currentTimeMillis() - Prefs.lockedAt(this));
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, Math.max(remaining, 1000L));
        return START_STICKY;
    }

    private void acquireWake() {
        try {
            PowerManager pm = getSystemService(PowerManager.class);
            if (wakeLock == null && pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "abonguard:alarm");
                wakeLock.acquire(60_000L);
            }
        } catch (Exception ignored) {}
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Alarm Absen",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);       // suara diputar sendiri lewat MediaPlayer
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setBypassDnd(true);
        nm.createNotificationChannel(ch);

        PendingIntent full = PendingIntent.getActivity(this, 3, lockIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Waktunya absen!")
                .setContentText("Buka ABON Sumbar dan tetap di sana " + Prefs.durationSec(this) + " detik")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(full)
                .setFullScreenIntent(full, true)
                .build();
    }

    private void startSound() {
        try {
            AudioManager am = getSystemService(AudioManager.class);
            am.setStreamVolume(AudioManager.STREAM_ALARM,
                    am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        } catch (Exception ignored) {}

        if (player == null) {
            try {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                player = new MediaPlayer();
                player.setAudioAttributes(ALARM_ATTRS);
                player.setDataSource(this, uri);
                player.setLooping(true);
                player.prepare();
                player.start();
            } catch (Exception e) {
                if (player != null) { player.release(); player = null; }
            }
        }

        if (vibrator == null) vibrator = getSystemService(Vibrator.class);
        if (vibrator != null) {
            vibrator.cancel();
            long[] pattern = {0, 900, 400};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), ALARM_ATTRS);
        }
    }

    private void stopSound() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
        if (vibrator != null) vibrator.cancel();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(timeout);
        stopSound();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }
}
