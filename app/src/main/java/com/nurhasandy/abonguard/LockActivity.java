package com.nurhasandy.abonguard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Layar penuh yang muncul jam 07:20 dan tidak bisa ditutup. */
public class LockActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#7F1D1D"));
        int pad = (int) (28 * d);
        root.setPadding(pad, pad, pad, pad);

        TextView time = new TextView(this);
        time.setText(String.format(Locale.US, "%02d:%02d", Prefs.hour(this), Prefs.minute(this)));
        time.setTextColor(Color.WHITE);
        time.setTextSize(64);
        time.setTypeface(Typeface.DEFAULT_BOLD);
        time.setGravity(Gravity.CENTER);
        root.addView(time);

        TextView msg = new TextView(this);
        msg.setText("WAKTUNYA ABSEN!\n\nSemua aplikasi lain dikunci.\nBuka ABON Sumbar dan tetap di dalamnya "
                + Prefs.durationSec(this) + " detik sampai alarm berhenti.");
        msg.setTextColor(Color.WHITE);
        msg.setTextSize(18);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, (int) (16 * d), 0, (int) (32 * d));
        root.addView(msg);

        Button open = new Button(this);
        open.setText("BUKA ABON SUMBAR");
        open.setTextSize(20);
        open.setTypeface(Typeface.DEFAULT_BOLD);
        open.setOnClickListener(v -> openTarget());
        root.addView(open, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (72 * d)));

        Button call = new Button(this);
        call.setText("Telepon darurat");
        call.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (24 * d);
        root.addView(call, lp);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Prefs.locked(this)) finish();
    }

    private void openTarget() {
        String t = Prefs.target(this);
        Intent li = t == null ? null : getPackageManager().getLaunchIntentForPackage(t);
        if (li == null) {
            Toast.makeText(this, "Aplikasi ABON tidak ditemukan. Kunci dilepas.", Toast.LENGTH_LONG).show();
            Scheduler.release(this, false);
            finish();
            return;
        }
        startActivity(li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        // Tanpa aksesibilitas, waktu 1 menit tidak bisa dipantau -> cukup buka ABON
        if (!Scheduler.accessibilityOn(this)) Scheduler.release(this, true);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Buka ABON Sumbar dulu", Toast.LENGTH_SHORT).show();
    }
}
