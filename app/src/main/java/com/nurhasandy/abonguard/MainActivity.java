package com.nurhasandy.abonguard;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView status;
    private Button btnTarget, btnTime, btnNotif, btnExact, btnAccess, btnOverlay, btnFull, btnBattery;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, pad);
        col.addView(status);

        col.addView(header("Pengaturan"));
        btnTarget = button(col, "", v -> pickApp());
        btnTime = button(col, "", v -> new TimePickerDialog(this, (tp, h, m) -> {
            Prefs.setTime(this, h, m);
            refresh();
        }, Prefs.hour(this), Prefs.minute(this), true).show());

        CheckBox weekdays = new CheckBox(this);
        weekdays.setText("Hanya hari kerja (Senin–Jumat)");
        weekdays.setChecked(Prefs.weekdaysOnly(this));
        weekdays.setOnCheckedChangeListener((cb, on) -> { Prefs.setWeekdaysOnly(this, on); refresh(); });
        col.addView(weekdays);

        col.addView(header("Izin yang wajib (ketuk yang bertanda ✗)"));
        btnAccess = button(col, "", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        btnNotif = button(col, "", v -> {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        });
        btnFull = button(col, "", v -> {
            if (Build.VERSION.SDK_INT >= 34) open(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri()));
        });
        btnOverlay = button(col, "", v -> open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri())));
        btnExact = button(col, "", v -> {
            if (Build.VERSION.SDK_INT >= 31) open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri()));
        });
        btnBattery = button(col, "", v -> open(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri())));

        col.addView(header("Uji coba"));
        button(col, "Uji sekarang (alarm langsung bunyi)", v -> {
            if (!Scheduler.targetInstalled(this)) { toast("Pilih aplikasi ABON dulu"); return; }
            Scheduler.startLock(this, true);
        });

        TextView note = new TextView(this);
        note.setText("\nCatatan: kunci otomatis dilepas setelah 30 menit sebagai pengaman "
                + "(misalnya ABON error atau tidak ada internet). Pop-up izin, kamera, dan telepon "
                + "tetap boleh muncul saat di ABON.");
        note.setTextSize(13);
        col.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(col);
        setContentView(sv);

        if (Prefs.target(this) == null) autoDetect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        long next = Scheduler.schedule(this);
        String when = new SimpleDateFormat("EEEE, d MMM HH:mm", new Locale("id", "ID")).format(new Date(next));
        boolean ready = Scheduler.targetInstalled(this) && Scheduler.accessibilityOn(this);
        status.setText((ready ? "✓ Aktif" : "✗ Belum siap — lengkapi izin di bawah")
                + "\nAlarm berikutnya: " + when
                + (Prefs.doneToday(this) ? "\n(Absen hari ini sudah selesai)" : ""));
        status.setTypeface(Typeface.DEFAULT_BOLD);

        btnTarget.setText("Aplikasi absen: " + targetLabel());
        btnTime.setText(String.format(Locale.US, "Jam alarm: %02d:%02d", Prefs.hour(this), Prefs.minute(this)));

        btnAccess.setText(mark(Scheduler.accessibilityOn(this)) + " Aksesibilitas (pengunci aplikasi)");

        boolean notifOk = Build.VERSION.SDK_INT < 33
                || checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
        btnNotif.setText(mark(notifOk) + " Notifikasi");

        boolean fullOk = true;
        if (Build.VERSION.SDK_INT >= 34) fullOk = getSystemService(NotificationManager.class).canUseFullScreenIntent();
        btnFull.setText(mark(fullOk) + " Notifikasi layar penuh");

        btnOverlay.setText(mark(Settings.canDrawOverlays(this)) + " Tampil di atas aplikasi lain");

        boolean exactOk = Build.VERSION.SDK_INT < 31 || getSystemService(AlarmManager.class).canScheduleExactAlarms();
        btnExact.setText(mark(exactOk) + " Alarm tepat waktu");

        boolean battOk = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        btnBattery.setText(mark(battOk) + " Bebas optimasi baterai");
    }

    private String targetLabel() {
        String t = Prefs.target(this);
        if (t == null) return "(belum dipilih)";
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(t, 0)).toString();
        } catch (Exception e) {
            return t + " (tidak terpasang)";
        }
    }

    private List<ResolveInfo> launcherApps() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = new ArrayList<>(getPackageManager().queryIntentActivities(i, 0));
        PackageManager pm = getPackageManager();
        Collections.sort(list, (a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        return list;
    }

    private void autoDetect() {
        PackageManager pm = getPackageManager();
        for (ResolveInfo ri : launcherApps()) {
            String label = ri.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
            String pkg = ri.activityInfo.packageName.toLowerCase(Locale.ROOT);
            if (label.contains("abon") || pkg.contains("abon")) {
                Prefs.setTarget(this, ri.activityInfo.packageName);
                return;
            }
        }
    }

    private void pickApp() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = launcherApps();
        List<String> pkgs = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (ResolveInfo ri : apps) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || pkgs.contains(pkg)) continue;
            pkgs.add(pkg);
            labels.add(ri.loadLabel(pm).toString());
        }
        new AlertDialog.Builder(this)
                .setTitle("Pilih aplikasi ABON Sumbar")
                .setItems(labels.toArray(new String[0]), (dlg, which) -> {
                    Prefs.setTarget(this, pkgs.get(which));
                    refresh();
                })
                .show();
    }

    private Uri pkgUri() { return Uri.parse("package:" + getPackageName()); }

    private void open(Intent i) {
        try { startActivity(i); } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri())); }
            catch (Exception ignored) {}
        }
    }

    private String mark(boolean ok) { return ok ? "✓" : "✗"; }

    private TextView header(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 4);
        return t;
    }

    private Button button(LinearLayout parent, String text, android.view.View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setOnClickListener(l);
        parent.addView(btn);
        return btn;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
