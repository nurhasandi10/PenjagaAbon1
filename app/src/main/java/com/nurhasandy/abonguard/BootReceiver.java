package com.nurhasandy.abonguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Scheduler.schedule(c);
        // HP di-restart saat masih terkunci -> kunci lagi (kecuali batas 30 menit sudah lewat)
        if (Prefs.locked(c)) {
            if (System.currentTimeMillis() - Prefs.lockedAt(c) < Prefs.MAX_LOCK_MS) {
                try {
                    c.startForegroundService(new Intent(c, AlarmService.class));
                } catch (Exception ignored) {}
            } else {
                Prefs.setLocked(c, false, false);
            }
        }
    }
}
