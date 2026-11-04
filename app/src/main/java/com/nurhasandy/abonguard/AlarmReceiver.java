package com.nurhasandy.abonguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Scheduler.schedule(c); // jadwal hari berikutnya

        if (Prefs.doneToday(c)) return;
        if (!Scheduler.targetInstalled(c)) return; // jangan mengunci kalau ABON tidak ada
        if (Prefs.weekdaysOnly(c)) {
            int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            if (d == Calendar.SATURDAY || d == Calendar.SUNDAY) return;
        }
        Scheduler.startLock(c, false);
    }
}
