package com.nurhasandy.abonguard;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Semua pengaturan & status kunci disimpan di sini. */
public final class Prefs {
    /** Batas aman: kunci otomatis dilepas setelah 30 menit (misal ABON error / tanpa internet). */
    public static final long MAX_LOCK_MS = 30L * 60L * 1000L;

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("abon_guard", Context.MODE_PRIVATE);
    }

    public static String target(Context c) { return sp(c).getString("target", null); }
    public static void setTarget(Context c, String pkg) { sp(c).edit().putString("target", pkg).apply(); }

    public static int hour(Context c) { return sp(c).getInt("hour", 7); }
    public static int minute(Context c) { return sp(c).getInt("minute", 20); }
    public static void setTime(Context c, int h, int m) {
        sp(c).edit().putInt("hour", h).putInt("minute", m).apply();
    }

    public static int durationSec(Context c) { return sp(c).getInt("duration", 60); }

    public static boolean weekdaysOnly(Context c) { return sp(c).getBoolean("weekdays", true); }
    public static void setWeekdaysOnly(Context c, boolean v) { sp(c).edit().putBoolean("weekdays", v).apply(); }

    public static boolean locked(Context c) { return sp(c).getBoolean("locked", false); }
    public static boolean isTest(Context c) { return sp(c).getBoolean("test", false); }
    public static long lockedAt(Context c) { return sp(c).getLong("locked_at", 0L); }

    public static void setLocked(Context c, boolean locked, boolean test) {
        SharedPreferences.Editor e = sp(c).edit().putBoolean("locked", locked);
        if (locked) e.putLong("locked_at", System.currentTimeMillis()).putBoolean("test", test);
        e.commit();
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static boolean doneToday(Context c) { return today().equals(sp(c).getString("done", "")); }
    public static void markDone(Context c) { sp(c).edit().putString("done", today()).apply(); }
}
