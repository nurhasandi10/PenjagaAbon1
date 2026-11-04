package com.nurhasandy.abonguard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.Calendar;

public final class Scheduler {
    private Scheduler() {}

    /** Jadwalkan alarm berikutnya. Mengembalikan waktu alarm (millis). */
    public static long schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, Prefs.hour(c));
        cal.set(Calendar.MINUTE, Prefs.minute(c));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        long t = cal.getTimeInMillis();

        PendingIntent pi = PendingIntent.getBroadcast(c, 1, new Intent(c, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent show = PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            } else {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show), pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        }
        return t;
    }

    public static boolean targetInstalled(Context c) {
        String t = Prefs.target(c);
        if (t == null) return false;
        try {
            c.getPackageManager().getPackageInfo(t, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean accessibilityOn(Context c) {
        String enabled = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        String me = c.getPackageName() + "/" + GuardService.class.getName();
        String meShort = c.getPackageName() + "/.GuardService";
        for (String s : enabled.split(":")) {
            if (s.equalsIgnoreCase(me) || s.equalsIgnoreCase(meShort)) return true;
        }
        return false;
    }

    /** Mulai kunci: bunyi, getar, layar kunci. */
    public static void startLock(Context c, boolean test) {
        Prefs.setLocked(c, true, test);
        Intent s = new Intent(c, AlarmService.class);
        c.startForegroundService(s);
    }

    public static void setSound(Context c, boolean on) {
        if (!Prefs.locked(c)) return;
        Intent i = new Intent(c, AlarmService.class)
                .setAction(on ? AlarmService.ACT_RESUME : AlarmService.ACT_SILENCE);
        try {
            c.startService(i);
        } catch (Exception e) {
            try { c.startForegroundService(i); } catch (Exception ignored) {}
        }
    }

    /** Lepas kunci. success = benar-benar sudah 1 menit di ABON. */
    public static void release(Context c, boolean success) {
        if (success && !Prefs.isTest(c)) Prefs.markDone(c);
        Prefs.setLocked(c, false, false);
        c.stopService(new Intent(c, AlarmService.class));
    }
}
