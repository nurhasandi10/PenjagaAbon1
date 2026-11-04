# Penjaga ABON

Jam 07:20 (bisa diubah) HP berbunyi keras + bergetar dan semua aplikasi dikunci.
Satu-satunya jalan keluar: buka **ABON Sumbar** dan tetap di dalamnya **1 menit**.
Keluar dari ABON sebelum 1 menit → alarm bunyi lagi dan hitungan diulang dari nol.

## Cara mendapatkan APK

**Opsi A — GitHub (tanpa install apa pun):**
1. Buat repository baru di github.com, unggah semua isi folder ini.
2. Buka tab **Actions** → workflow "Build APK" berjalan otomatis (±5 menit).
3. Unduh artefak **PenjagaAbon-apk** → ekstrak → `app-debug.apk`.

**Opsi B — Android Studio:** File → Open → pilih folder ini → Build → Build APK(s).

## Setelah install
Buka aplikasi, lalu ketuk setiap tombol bertanda ✗ sampai semuanya ✓:
- Aplikasi absen: pastikan terpilih ABON Sumbar
- Aksesibilitas → pilih "Penjaga ABON" → aktifkan (wajib, inilah pengunci aplikasinya)
- Notifikasi, notifikasi layar penuh, tampil di atas aplikasi lain, alarm tepat, bebas optimasi baterai

Di HP Xiaomi/Oppo/Vivo/Realme, aktifkan juga **Autostart** dan setel baterai ke **Tanpa batasan**,
kalau tidak alarm bisa dimatikan sistem.

Ketuk **Uji sekarang** untuk mencoba (uji coba tidak menghitung absen hari ini).

## Pengaman
- Kunci otomatis lepas setelah 30 menit (ABON error / tanpa internet).
- Pop-up izin, kamera, GPS, dan telepon tetap boleh muncul; tombol "Telepon darurat" selalu ada.
- Layar mati saat di ABON tidak dihitung sebagai waktu.
