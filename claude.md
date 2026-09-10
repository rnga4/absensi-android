# Absensi Android App - Context & Developer Guide (`claude.md`)

This document provides a concise overview of the codebase, project architecture, build commands, and UI design guidelines for LLM agents and human developers.

---

## 📱 App Overview

**Absensi Android** is a native Android application built in **Kotlin** for tracking employee attendance. It connects to internal company APIs, displays employees who haven't checked in yet grouped by department, handles search filtering, and runs a background WorkManager worker for periodic attendance alerts.

- **Primary Package**: `com.unico.absensi`
- **Output APK**: `absensi.apk` (at root directory `/home/orion3/android-app/absensi-android/absensi.apk`)
- **Launcher Entry Point**: `LoginActivity` (bukan `MainActivity`). Jika user sudah login, `LoginActivity` otomatis redirect ke `AdminActivity` (role admin) atau `EmployeeActivity` (role employee). Tombol "Publik" di login screen membuka `MainActivity` tanpa sesi.

### App Flow
```
LoginActivity (LAUNCHER) ─┬─ [sudah login] ──→ AdminActivity / EmployeeActivity
                          └─ [tombol Publik] ─→ MainActivity (tanpa sesi)
MainActivity (index publik) ──→ tap avatar → showPhotoPreviewDialog()
                                tap vote ❤️ → (jika belum login) → LoginActivity
EmployeeActivity ──→ tap avatar → ProfileSettingsActivity
                  ──→ btnHistory → HistoryActivity
AdminActivity ──→ tap employee → HistoryActivity
```

---

## 🛠️ Build & Verification Commands

```bash
# Build Debug APK
./gradlew assembleDebug --no-daemon

# Clean & Build Debug APK
./gradlew clean assembleDebug --no-daemon

# Copy output APK to root directory
cp app/build/outputs/apk/debug/app-debug.apk absensi.apk
```

---

## 🎨 UI Design System - Komi Store Aesthetic (`komistore.app`)

The UI follows the **Komi Store (`komistore.app`)** design philosophy:
> *Modern Minimalist SaaS + Editorial Swiss-Style Typography + Subtle Neo-Brutalist Elements*

### Key Visual Tokens & Guidelines

1. **Color Palette (`colors.xml`)**:
   - `bg_primary`: Off-white neutral page background (`#FAFAFA`)
   - `surface`: Pure white card background (`#FFFFFF`)
   - `text_primary`: Pitch black high contrast (`#09090B`)
   - `text_secondary`: Clean slate gray (`#71717A`)
   - `border_crisp`: Crisp solid border stroke (`#E4E4E7`)
   - `border_dark`: High contrast border stroke (`#18181B`)
   - `badge_danger_bg` (`#FEF2F2`), `badge_danger_text` (`#DC2626`), `badge_danger_border` (`#FCA5A5`)

2. **Typography & Swiss Editorial Layout**:
   - **Hero Headline**: `28sp` bold pitch black (`#09090B`) with `-0.03` letter spacing.
   - **Section Headers**: Uppercase Swiss notation (e.g. `/// TEKNOLOGI  —  4 KARYAWAN`) with `0.10` letter spacing.
   - **Technical Pills**: Monospace-feel tags like `[ SYSTEM / LIVE FEED ]` or `[ BELUM ABSEN ]`.

3. **Card & Component Geometry**:
   - **Item Cards**: `1.5dp` solid stroke (`#E4E4E7`), `10dp` corner radius, zero heavy shadows.
   - **Avatar Badges**: Rounded-square (`8dp` corner radius) with `1.5dp` solid border (`#18181B`) and bold white initials.
   - **Hero Container**: Minimalist card container with `1.5dp` stroke (`#E4E4E7`).

---

## 📂 Key File Structure

```
absensi-android/
├── absensi.apk                  # Production/Debug compiled APK shortcut
├── claude.md                    # Context & developer guidelines (this file)
├── app/
│   ├── build.gradle.kts         # App dependencies (OkHttp, WorkManager, Material)
│       └── src/main/
│               ├── AndroidManifest.xml  # Manifest with permissions & WorkManager setup
│               ├── assets/LICENSE.txt   # Teks MIT + copyright (ikut ter-package di APK)
│           ├── java/com/unico/absensi/
│           │   ├── MainActivity.kt        # Index publik: daftar belum absen, search, refresh animation, vote ❤️
│           │   ├── AbsensiAdapter.kt      # Adapter index publik (Swiss headers, avatar foto bulat, vote button)
│           │   ├── Models.kt              # ListRow (DeptHeader & Employee + loveCount/hasLoved), VoteResult, UserProfile
│           │   ├── AbsensiWorker.kt       # Periodic WorkManager worker (30 min interval)
│           │   ├── NotificationHelper.kt  # Notification channel & manager builder
│           │   ├── CircularPhoto.kt       # Helper bitmap to circular drawable
│           │   ├── ExitHelper.kt          # Konfirmasi dialog "Keluar Aplikasi?" saat tombol back
│           │   ├── LoginActivity.kt       # Login (api_login.php), route ke Admin/Employee
│           │   ├── AdminActivity.kt       # Dashboard admin (stat cards, filter, search)
│           │   ├── AdminAdapter.kt        # Adapter admin (avatar foto karyawan bulat + cache)
│           │   ├── EmployeeActivity.kt    # Halaman profil user normal (avatar tap → pengaturan)
│           │   ├── HistoryActivity.kt     # Riwayat kehadiran (pagination)
│           │   ├── HistoryAdapter.kt      # Adapter riwayat
│           │   ├── ProfileSettingsActivity.kt # Pengaturan profil (ganti foto & password)
│           │   ├── ApiClient.kt           # OkHttp client, cookie PHPSESSID persist, multipart upload, vote POST
│           │   ├── ApiConfig.kt           # Base URLs failover + endpoint paths (VOTE: api_vote.php)
│           │   ├── AbsensiApi.kt          # Wrapper API (login/dashboard/history/profile/photo/vote)
│           │   ├── LogoutHelper.kt        # Konfirmasi logout
│           │   └── Prefs.kt               # Sesi login (SharedPreferences)
│           └── res/
│               ├── drawable/
│               │   ├── bg_avatar_circle.xml # Avatar bulat (oval + stroke) — dipakai semua avatar
│               │   ├── bg_avatar_box.xml    # (legacy rounded square)
│               │   ├── bg_vote_active.xml   # Soft red pill active state (❤️ vote)
│               │   ├── bg_vote_inactive.xml # Crisp gray pill inactive state (❤️ vote)
│               │   ├── bg_hero_card.xml    # Minimalist SaaS hero card container
│               │   ├── bg_search.xml       # Crisp search input field shape
│               │   ├── bg_tag_pill.xml     # Technical pill tag background
│               │   └── bg_badge.xml        # Technical status badge shape
│               ├── layout/
│               │   ├── activity_main.xml          # Komi Store header & search layout
│               │   ├── activity_employee.xml      # Profil user: avatar bulat, status, tombol
│               │   ├── activity_profile_settings.xml # Ganti foto + ganti password
│               │   ├── activity_login.xml         # Login screen + tombol "Publik"
│               │   ├── activity_admin.xml         # Dashboard admin (stat cards, filter)
│               │   ├── activity_history.xml       # Riwayat kehadiran
│               │   ├── dialog_employee_photo.xml  # Modal foto zoom (controls, spinner, fallback)
│               │   ├── item_employee.xml          # Item index publik (avatar foto bulat + vote pill + crown badge)
│               │   ├── item_admin_employee.xml    # Item admin (avatar foto bulat + badge status)
│               │   ├── item_department_header.xml # Section header Swiss (/// DEPT — N)
│               │   ├── item_history.xml           # Item riwayat (date, in/out, badge)
│               │   └── item_stat.xml              # Stat card item
│               └── values/
│                   └── colors.xml                 # Design tokens (Monochrome & Badges)
```

---

## 🌐 API & Network Infrastructure

The app queries API endpoints with automatic failover fallback and dynamic working IP prioritization:
1. Primary: `http://192.168.1.37:9790`
2. Secondary: `http://100.102.13.11:9790`

- **Dynamic IP Prioritization**: Konek terakhir yang sukses disimpan di `Prefs.saveLastBaseUrl(...)`. Saat app dibuka, URL yang sukses tersebut dicoba **pertama kali** (menghindari timeout TCP SYN 4 detik di Android 15 ketika berada di luar Wi-Fi lokal).
- **Instant Cache Rendering**: Data JSON publik disimpan di `Prefs.saveCachedPublicJson(...)`. Begitu aplikasi dibuka, UI langsung di-render secara **instan (0 ms)** dari cache lokal sambil melakukan background syncing.
- **HTTP Timeouts**: Connect 2.5s, Read 4-8s (bervariasi antar client instance).

### Complete API Endpoints (defined in `ApiConfig.kt`)

| Constant | Path | Auth | Method | Fungsi |
|---|---|---|---|---|
| `LOGIN` | `/api_login.php` | No | POST form | Login → `{ success, role, username, name, emp_code, dept }` |
| `PUBLIC` | `/api_public.php` | No | GET | Daftar karyawan belum absen (cache + failover) |
| `DASHBOARD` | `/api_dashboard.php` | PHPSESSID | GET | Admin: stat + departments; Employee: self info |
| `HISTORY` | `/api_history.php` | PHPSESSID | GET `?code=&offset=` | Riwayat kehadiran (pagination, 30 item/req) |
| `PROFILE` | `/profile.php?format=json` | PHPSESSID | GET/POST | Profil, ganti password, upload foto |
| `VOTE` | `/api_vote.php` | PHPSESSID | POST form | Vote ❤️ toggle (`action=vote`, `emp_code`) |
| `LOGOUT` | `/logout.php` | PHPSESSID | GET | Destroy sesi server-side |
| — | `/photo.php?u=<username>` | PHPSESSID | GET | Foto profil by username (employee hanya foto sendiri) |
| — | `/photo.php?emp=<code>&pub=1` | No | GET | Foto publik by emp_code (untuk index publik & admin) |

### Expected Response JSON (`/api_public.php`)
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
        { 
          "emp_code": "E001", 
          "name": "Budi Santoso",
          "love_count": 2,
          "my_vote": true
        }
      ]
    }
  ]
}
```

---

## ❤️ Single Love Vote Feature (Apresiasi Karyawan Belum Absen)

Aplikasi Android terintegrasi dengan backend `api_vote.php` untuk fitur apresiasi harian:
- **Endpoint**: `POST /api_vote.php` (`action=vote`, `emp_code=<code>`).
- **Autentikasi Sesi & Cookie Persistence**: Memerlukan sesi login (`PHPSESSID`). `MainActivity` dan `ApiClient` dikonfigurasi dengan `PersistentCookieStorage` (`OkHttpClient.cookieJar`) agar sesi login tetap bertahan saat *swipe-refresh* (`fetchData()`), mencegah status vote pengguna mereset kembali ke default (abu `🤍`). Jika user mengeklik tombol vote saat status *guest* (belum login), app akan memberikan notifikasi dan membuka `LoginActivity`.
- **State Toggle & Inline Count**: Memberi/menarik vote ❤️ secara realtime. Di-render secara inline (`tvVoteIcon` `❤️`/`🤍` + `tvVoteCount` angka vote bold) dengan feedback sentuh borderless (`?attr/selectableItemBackgroundBorderless`). Respon server `{ success, state ('added'|'removed'), my_vote, love_count }` langsung memperbarui UI tanpa reload ulang seluruh daftar.

---

## 👤 Profil & Pengaturan (Profile/Settings)

Semua data profil (foto & password) disimpan di **server** (SQLite `app/data/users.sqlite` + file `data/photos/`), sehingga **web & Android otomatis sinkron** — ganti di web ⇒ tampil di HP & sebaliknya.

### Endpoint (semua pakai sesi login / cookie PHPSESSID)
| Endpoint | Method | Fungsi |
|---|---|---|
| `profile.php?format=json` | GET | Ambil profil: `{ name, username, emp_code, dept, role, has_photo, photo_url }` |
| `profile.php?format=json` | POST `action=password` | Ganti password (`current_password`, `new_password`, `confirm_password`), min 6 char |
| `profile.php?format=json` | POST multipart `action=photo` + file `photo` | Upload foto profil (max 2MB, jpg/png/webp) |
| `photo.php?u=<username>` | GET | Ambil foto (perlu sesi; employee hanya fotonya sendiri) |
| `photo.php?emp=<emp_code>&pub=1` | GET | Ambil foto publik by emp_code (dipakai index publik & admin) |
| `api_vote.php` | POST | Vote ❤️ karyawan (`action=vote`, `emp_code`) |

### Alur
- **Halaman profil user normal** (`EmployeeActivity`): avatar bulat menampilkan foto asli & jumlah akumulasi love vote (`❤️ X`); **ketuk avatar** → buka `ProfileSettingsActivity`. Penguatan siklus hidup `onResume()` memastikan foto profil yang baru diganti di `ProfileSettingsActivity` langsung di-fetch dan ter-update secara *real-time* tanpa perlu menutup aplikasi.
- **`ProfileSettingsActivity`**: tombol **Ganti Foto** (pilih dari galeri → dimampatkan ke ≤2400px → upload multipart → `setResult(RESULT_OK)` + tampil langsung di layar), form **ganti password** (lama / baru / konfirmasi + pesan error), tombol kembali. **Kartu `[ TENTANG & LISENSI ]`** di bawah form password menampilkan versi app (`BuildConfig.VERSION_NAME`, butuh `buildConfig = true` di build.gradle.kts) + copyright `© 2026 rnga4`; ketuk `tvLicense` → dialog MIT yang dibaca dari `assets/LICENSE.txt` + kredit library pihak ketiga.
- **Index Publik** (`MainActivity`/`AbsensiAdapter`) & **Dashboard Admin** (`AdminAdapter`): avatar menampilkan **foto karyawan bulat** (di-cache per emp_code, dimuat async, `itemView.post` saat selesai — jangan panggil `notifyItemChanged` dari thread background). Fallback inisial lingkaran berwarna.
- **Tampilan Vote Love**: Format minimalis emote + angka (`❤️ X`) di-render di index publik, modal dialog profil web `public.php`, halaman profil web `employee.php`, dan halaman profil Android `EmployeeActivity`.

### Konvensi
- Foto profil dibuat bulat dengan **`RoundedBitmapDrawableFactory.create()` + `isCircular = true`** (helper `com.unico.absensi.CircularPhoto.kt` → `Bitmap.toCircularDrawable(resources)`), lalu `ivAvatar.setImageDrawable(...)`. Helper ini dipakai bersama di halaman profil user, Index Publik, dashboard admin, dan pengaturan profil. **JANGAN bergantung pada `android:clipToOutline`** untuk memotong foto — tidak konsisten di Android 11 (API ≤30) sehingga foto tampil kotak; `setCircular` bekerja bulat di semua versi. *Catatan: `ProfileSettingsActivity` tetap memakai `clipToOutline = true` pada background oval avatar, tapi hanya untuk shape fallback — foto aktual tetap di-render via `toCircularDrawable()`.*
- Pemuatan profil dan foto pada `EmployeeActivity` ditempatkan di dalam handler **`onResume()`** agar setiap kali kembali dari layar pengaturan foto (`ProfileSettingsActivity`), avatar dan data pengguna langsung ter-refresh otomatis.
- **`EmployeeActivity` auto-refresh status absen tiap 60 detik** — `refreshTodayStatus()` hanya update data `today` dari `api_dashboard.php` tanpa animasi/flicker. Full reload (`loadData()`: profile + foto + animasi) hanya saat `onResume`. Ada guard `isLoading` agar tidak double-fetch.
- **`AdminActivity` hanya fetch data di `onCreate()`** (tidak reload di `onResume()`). Berbeda dengan `EmployeeActivity` yang refresh setiap `onResume()`. Jika data admin perlu di-update, user harus swipe-to-refresh manual.
- TextView inisial sudah bulat via `shape="oval"` (tidak butuh clip). Background oval di XML boleh dibiarkan sebagai fallback.
- Untuk foto di adapter: selalu `photoCache[synchronizedMap]` + `Thread` + `itemView.post {}` (hindari `CalledFromWrongThreadException`).

### Session & Cache Behavior
- **`Prefs.clear()`** (dipanggil saat logout) secara eksplisit **mempertahankan** `last_base_url` dan `cached_public_json` — sehingga setelah logout, app masih menampilkan data publik terakhir & tetap memakai IP server yang terakhir berhasil dijangkau. Ini disengaja untuk UX agar layar publik tidak kosong setelah logout.
- **`PersistentCookieStorage`** hanya menyimpan cookie `PHPSESSID`. Saat logout, cookie di-clear dari SharedPreferences `absensi_cookies`.

---

## 🔍 Interactive Photo Preview Modal & Crown Badge (#1 Top Voted)

### 1. 🖼️ Interactive Photo Zoom Modal
- **Trigger**: Menekan foto/avatar (`flAvatar`, `ivAvatar`, `tvAvatar`) pada daftar Public Index (Web & Android) akan memunculkan dialog modal foto profil resolusi penuh.
- **Android (`dialog_employee_photo.xml` / `MainActivity.kt`)**: Menyediakan tombol kontrol zoom (`+`, `-`, `Reset`), indikator rasio skala (`100%`), loading spinner (`ProgressBar`), serta status fallback `tvNoPhoto` ("Tidak Ada Foto") jika karyawan belum mengunggah foto.
- **Container Touch Target (`flAvatar`)**: FrameLayout 40dp x 40dp pada `item_employee.xml` dibind di `AbsensiAdapter.kt` dengan `android:clickable="true"` dan `selectableItemBackgroundBorderless` ripple, menjamin sentuhan responsif di seluruh area foto/inisial.

### 2. 👑 Crown Badge di Atas Foto Profil (#1 Top Voted)
- Karyawan peraih **vote Love terbanyak #1** (`rank = 1`) secara otomatis mengenakan icon mahkota **`👑`** di atas foto profil mereka.
- **Android (`item_employee.xml` / `AbsensiAdapter.kt`)**: `tvCrownBadge` diposisikan melayang secara presisi di atas tengah bingkai avatar (`layout_gravity="top|center_horizontal"` dan `layout_marginTop="-11dp"`), memberikan efek visual karyawan sedang memakai mahkota.
- **PWA Web (`pwa/js/app.js` / `pwa/css/styles.css`)**: `.crown-badge` diposisikan melayang di atas bundar avatar (`top: -11px; left: 50%`) dengan efek bayangan alami `drop-shadow`.

