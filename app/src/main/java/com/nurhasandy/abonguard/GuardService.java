package com.nurhasandy.abonguard;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.telecom.TelecomManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * Mendeteksi aplikasi yang dibuka. Saat terkunci:
 * - aplikasi selain ABON (dan pengecualian sistem) -> langsung dilempar balik ke layar kunci
 * - di dalam ABON -> hitung mundur; setelah durasi terpenuhi, kunci dilepas
 */
public class GuardService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean inTarget = false;
    private long enteredAt = 0L;
    private Set<String> allowed;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!Prefs.locked(GuardService.this) || !inTarget) { inTarget = false; return; }
            PowerManager pm = getSystemService(PowerManager.class);
            // layar mati tidak dihitung -> timer diulang
            if (pm != null && !pm.isInteractive()) enteredAt = System.currentTimeMillis();

            long need = Prefs.durationSec(GuardService.this) * 1000L;
            if (System.currentTimeMillis() - enteredAt >= need) {
                inTarget = false;
                Scheduler.release(GuardService.this, true);
                Toast.makeText(GuardService.this, "Absen selesai. HP sudah bebas dipakai.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        buildAllowed();
    }

    /** Paket yang boleh muncul tanpa dianggap "kabur": sistem, izin, kamera, telepon. */
    private void buildAllowed() {
        allowed = new HashSet<>();
        allowed.add("com.android.systemui");
        allowed.add("android");
        allowed.add("com.android.permissioncontroller");
        allowed.add("com.google.android.permissioncontroller");
        allowed.add("com.android.packageinstaller");
        allowed.add("com.google.android.packageinstaller");
        allowed.add("com.google.android.gms");          // dialog lokasi/GPS
        allowed.add("com.android.server.telecom");
        allowed.add("com.android.incallui");
        allowed.add("com.google.android.dialer");
        allowed.add("com.samsung.android.incallui");
        try {
            TelecomManager tm = getSystemService(TelecomManager.class);
            if (tm != null && tm.getDefaultDialerPackage() != null) allowed.add(tm.getDefaultDialerPackage());
        } catch (Exception ignored) {}
        try { // aplikasi kamera (untuk selfie absen)
            for (ResolveInfo ri : getPackageManager().queryIntentActivities(
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)) {
                allowed.add(ri.activityInfo.packageName);
            }
        } catch (Exception ignored) {}
    }

    private boolean isActivity(String pkg, String cls) {
        try {
            getPackageManager().getActivityInfo(new ComponentName(pkg, cls), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!Prefs.locked(this)) { inTarget = false; return; }
        if (e.getPackageName() == null) return;
        if (allowed == null) buildAllowed();

        String pkg = e.getPackageName().toString();
        String target = Prefs.target(this);

        if (pkg.equals(target)) {
            if (!inTarget) {
                inTarget = true;
                enteredAt = System.currentTimeMillis();
                Scheduler.setSound(this, false);
                Toast.makeText(this, "Tetap di ABON selama " + Prefs.durationSec(this) + " detik",
                        Toast.LENGTH_LONG).show();
                handler.removeCallbacks(tick);
                handler.postDelayed(tick, 1000L);
            }
            return;
        }

        CharSequence cls = e.getClassName();
        if (cls == null || !isActivity(pkg, cls.toString())) return; // abaikan keyboard, toast, dll.

        if (allowed.contains(pkg)) return; // pop-up izin/kamera/telepon: timer tetap jalan

        // Keluar dari ABON -> timer diulang dari nol
        if (inTarget) {
            inTarget = false;
            handler.removeCallbacks(tick);
        }
        Scheduler.setSound(this, true);

        if (!pkg.equals(getPackageName())) {
            try { startActivity(AlarmService.lockIntent(this)); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onInterrupt() {}
}
