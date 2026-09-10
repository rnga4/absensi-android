# Absensi Android

> *Native Android app untuk monitoring kehadiran karyawan — login admin/staff, live feed, voting love, dark mode.*

Aplikasi Android native (Kotlin) dengan **multi-role** (admin & staff). Fitur login/session, live feed karyawan real-time, voting love, photo profile, riwayat kehadiran, pull-to-refresh, notifikasi WorkManager, haptics, dan desain **Komi Store** (`komistore.app`) — modern minimalist SaaS + Swiss-style typography.

---

## Fitur

### Auth & Role
- **Login multi-role** — admin (full monitoring) / staff (personal dashboard)
- **Session management** — cookie `PHPSESSID` tersimpan (`EncryptedSharedPreferences` atau plain fallback)
- **Auto-logout 401** — token expired → hapus sesi → navigasi ke LoginActivity

### Staff
- **Dashboard personal** — status IN/OUT, foto profil, badge hadir/belum
- **Ganti password** dari pengaturan profil
- **Riwayat kehadiran** (`HistoryActivity`) — date picker, foto presensi
- **Index Publik** — lihat daftar karyawan semua

### Admin / Public
- **Live feed** — daftar karyawan yang belum absen, dikelompokkan per departemen
- **Pencarian** nama karyawan (filter dinamis per departemen)
- **Pull-to-refresh custom** — progress strip Swiss + badge `[ SYNCING... ]` → `[ DONE ]`

### Social & UI
- **Voting love** — tap ❤️ untuk dukung karyawan, guard double-vote
- **Foto profil** — upload dari galeri/kamera, preview pinch-to-zoom
- **Haptics** di semua titik tap (VibrationEffect + fallback)
- **Dark mode** otomatis (DayNight)
- **Dialog lisensi MIT** — custom animated modal dengan header gradient
- **Tombol GitHub Star** — redirect ke repo untuk star

### Background & Notification
- **WorkManager** periodic alert (jam 06–22, maks 1× per 3 jam)
- **Notifikasi** via `NotificationChannel` + `NotificationCompat`

### Networking
- **Failover otomatis** antar endpoint API (2 server LAN/Tailscale)
- **Client terpisah** — upload (read/write timeout 30s) & regular

---

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Min / Target SDK | 24 / 34 |
| UI | XML + ConstraintLayout + RecyclerView + SwipeRefreshLayout, Material |
| Networking | OkHttp 4.12 |
| Background | WorkManager (PeriodicWorkRequest) |
| Notifikasi | NotificationChannel + NotificationCompat |
| Storage | EncryptedSharedPreferences (Security-Crypto) + SharedPreferences fallback |
| Build | Gradle Kotlin DSL, viewBinding, buildConfig |
| CI/CD | GitHub Actions (`build-apk.yml`) |

---

## Build

```bash
# Debug
./gradlew assembleDebug --no-daemon

# Release (butuh keystore.properties — copy dari keystore.properties.example)
./gradlew assembleRelease --no-daemon

# Output
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

**Release signing** (opsional): buat `keystore.properties` dari template, isi path keystore + credentials. Kalau tidak ada, fallback ke debug signing.

---

## API

App memanggil endpoint dengan **failover otomatis**:
1. `http://192.168.1.37:9790/api_public.php` (LAN)
2. `http://100.102.13.11:9790/api_public.php` (Tailscale)

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

## Struktur Utama

```
app/src/main/
├── AndroidManifest.xml
├── java/com/unico/absensi/
│   ├── MainActivity.kt          # Staff dashboard + live feed admin
│   ├── LoginActivity.kt         # Login multi-role + cookie session
│   ├── HistoryActivity.kt       # Riwayat kehadiran + date picker
│   ├── ProfileSettingsActivity.kt # Ganti password, profil, lisensi
│   ├── EmployeeActivity.kt      # Live feed admin (SwipeRefresh)
│   ├── AbsensiAdapter.kt        # RecyclerView adapter (Swiss headers & avatars)
│   ├── AbsensiApi.kt            # API calls, LruCache foto, voting
│   ├── ApiConfig.kt             # Endpoint failover (static baseUrls)
│   ├── ApiClient.kt             # OkHttp singleton, 401 handler, cookie store
│   ├── Models.kt                # Data classes (Employee, Attendance, LoginResponse)
│   ├── Prefs.kt                 # SharedPreferences + session cache
│   ├── Ui.kt                    # runOnUiThreadSafe + utilities
│   ├── Haptics.kt               # VibrationEffect wrapper
│   ├── AbsensiWorker.kt         # WorkManager periodic notification
│   └── NotificationHelper.kt    # Notification channel & builder
├── res/
│   ├── anim/                    # dialog_in/out (pop animation)
│   ├── drawable/                # Shapes: bg_hero_card, bg_tag_pill, bg_accent_bar, icons
│   ├── layout/                  # activity_main, activity_login, dialog_license, etc.
│   ├── mipmap-*/                # Launcher icon (legacy + adaptive)
│   └── values/                  # colors, strings, themes (DayNight)
├── assets/
│   └── LICENSE.txt              # MIT license text
└── .github/workflows/
    └── build-apk.yml            # CI: assembleDebug + assembleRelease
```

---

## Release / APK

- `versionCode 2` · `versionName "1.1.0"`
- `applicationId: com.unico.absensi`
- CI build otomatis via GitHub Actions per push ke `main`

---

## Lisensi

Dilisensikan di bawah **MIT License** — bebas dipakai, dimodifikasi, dan didistribusikan dengan tetap mencantumkan atribusi.

Copyright (c) 2026 rnga4

Lihat [LICENSE](LICENSE) untuk detail lengkap.
