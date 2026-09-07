# 📱 Absensi Android

> *Native Android app untuk tracking absensi karyawan — Swiss editorial, monokrom, & modern-minimalist.*

Aplikasi Android native (Kotlin) untuk memantau karyawan yang **belum absen**. Terhubung ke API internal, menampilkan daftar per departemen dengan orientasi desain **Komi Store** (`komistore.app`) — modern minimalist SaaS + Swiss-style typography + subtle neo-brutalist.

---

## ✨ Fitur

- 📋 **Daftar real-time** karyawan yang belum absen, dikelompokkan per departemen
- 🔍 **Pencarian** nama karyawan (filter dinamis per departemen)
- 🔄 **Pull-to-refresh custom** — progress strip Swiss yang mengisi + badge status `[ SYNCING... ]` → `[ DONE ]`
- 🔔 **Notifikasi berkala** via WorkManager (interval 30 menit)
- 🌐 **Failover otomatis** antar endpoint API (jika satu mati, coba yang lain)
- 📱 **Adaptive launcher icon** untuk semua ukuran layar

---

## 🛠️ Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Min / Target SDK | 24 / 34 |
| UI | XML + ConstraintLayout + RecyclerView, Material |
| Networking | OkHttp 4.12 |
| Background | WorkManager (PeriodicWorkRequest) |
| Notifikasi | NotificationChannel + NotificationCompat |

---

## 🏗️ Build

```bash
# Build Debug APK
./gradlew assembleDebug --no-daemon

# Clean & build
./gradlew clean assembleDebug --no-daemon

# Salin APK ke root (absensi.apk)
cp app/build/outputs/apk/debug/app-debug.apk absensi.apk
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🌐 API

App memanggil endpoint dengan **failover otomatis**:
1. `http://192.168.1.37:9790/api_public.php`
2. `http://100.102.13.11:9790/api_public.php`

**Response JSON:**
```json
{
  "date": "2026-09-07",
  "time": "13:11:14",
  "total_not_absen": 3,
  "all_present": false,
  "departments": [
    {
      "department": "Teknologi",
      "employees": [
        { "emp_code": "E001", "name": "Budi Santoso" }
      ]
    }
  ]
}
```

---

## 🎨 Design System — Komi Store Aesthetic

> *Modern Minimalist SaaS + Editorial Swiss-Style Typography + Subtle Neo-Brutalist Elements*

**Colors** (`colors.xml`):
- `bg_primary` `#FAFAFA` · `surface` `#FFFFFF` · `text_primary` `#09090B` · `text_secondary` `#71717A`
- Stroke tegas `1.5dp` (`border_crisp` `#E4E4E7`), avatar monokrom + badge status merah/hijau

**Typography & Layout:**
- Hero headline `28sp` bold `#09090B`, letter-spacing `-0.03`
- Section header Swiss: `/// TEKNOLOGI — 4 KARYAWAN` (uppercase, spacing `0.10`)
- Technical pills: `[ SYSTEM / LIVE FEED ]`, `[ BELUM ABSEN ]`, `[ SYNCING... ]`

---

## 📁 Struktur Utama

```
app/src/main/
├── AndroidManifest.xml
├── java/com/unico/absensi/
│   ├── MainActivity.kt        # View logic, search filter, pull-to-refresh
│   ├── AbsensiAdapter.kt      # RecyclerView adapter (Swiss headers & avatars)
│   ├── Models.kt              # ListRow sealed class (DeptHeader & Employee)
│   ├── AbsensiWorker.kt       # WorkManager periodic alert (30 menit)
│   └── NotificationHelper.kt  # Notification channel & builder
└── res/
    ├── drawable/              # Shapes: badge, avatar, tag pill, refresh strip, icon fg/bg
    ├── layout/                # activity_main, item_employee, item_department_header
    ├── mipmap-*/              # Launcher icon (legacy + adaptive)
    └── values/                # colors, strings, themes
```

---

## 📦 Release / APK

- `absensi.apk` — shortcut APK compiled di root project
- Versi: `1.0` (`versionCode 1`) · `applicationId: com.unico.absensi`

---

## 📄 Lisensi

© 2026 — Internal project. Untuk keperluan perusahaan/perorangan.
