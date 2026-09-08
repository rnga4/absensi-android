# Absensi Android App - Context & Developer Guide (`CLAUDE.md`)

This document provides a concise overview of the codebase, project architecture, build commands, and UI design guidelines for LLM agents and human developers.

---

## 📱 App Overview

**Absensi Android** is a native Android application built in **Kotlin** for tracking employee attendance. It connects to internal company APIs, displays employees who haven't checked in yet grouped by department, handles search filtering, and runs a background WorkManager worker for periodic attendance alerts.

- **Primary Package**: `com.unico.absensi`
- **Output APK**: `absensi.apk` (at root directory `/home/orion3/android-app/absensi-android/absensi.apk`)

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
├── CLAUDE.md                    # Context & developer guidelines
├── app/
│   ├── build.gradle.kts         # App dependencies (OkHttp, WorkManager, Material)
│       └── src/main/
│           ├── AndroidManifest.xml  # Manifest with permissions & WorkManager setup
│           ├── java/com/unico/absensi/
│           │   ├── MainActivity.kt        # Index publik: daftar belum absen, search, refresh animation
│           │   ├── AbsensiAdapter.kt      # Adapter index publik (Swiss headers, avatar foto bulat)
│           │   ├── Models.kt              # ListRow sealed class (DeptHeader & Employee) + UserProfile
│           │   ├── AbsensiWorker.kt       # Periodic WorkManager worker (30 min interval)
│           │   ├── NotificationHelper.kt  # Notification channel & manager builder
│           │   ├── LoginActivity.kt       # Login (api_login.php), route ke Admin/Employee
│           │   ├── AdminActivity.kt       # Dashboard admin (stat cards, filter, search)
│           │   ├── AdminAdapter.kt        # Adapter admin (avatar foto karyawan bulat + cache)
│           │   ├── EmployeeActivity.kt    # Halaman profil user normal (avatar tap → pengaturan)
│           │   ├── HistoryActivity.kt     # Riwayat kehadiran (pagination)
│           │   ├── HistoryAdapter.kt      # Adapter riwayat
│           │   ├── ProfileSettingsActivity.kt # Pengaturan profil (ganti foto & password)
│           │   ├── ApiClient.kt           # OkHttp client, cookie PHPSESSID persist, mulipart upload
│           │   ├── ApiConfig.kt           # Base URLs failover + endpoint paths
│           │   ├── AbsensiApi.kt          # Wrapper API (login/dashboard/history/profile/photo)
│           │   ├── LogoutHelper.kt        # Konfirmasi logout
│           │   └── Prefs.kt               # Sesi login (SharedPreferences)
│           └── res/
│               ├── drawable/
│               │   ├── bg_avatar_circle.xml # Avatar bulat (oval + stroke) — dipakai semua avatar
│               │   ├── bg_avatar_box.xml    # (legacy rounded square)
│               │   ├── bg_hero_card.xml    # Minimalist SaaS hero card container
│               │   ├── bg_search.xml       # Crisp search input field shape
│               │   ├── bg_tag_pill.xml     # Technical pill tag background
│               │   └── bg_badge.xml        # Technical status badge shape
│               ├── layout/
│               │   ├── activity_main.xml          # Komi Store header & search layout
│               │   ├── activity_employee.xml      # Profil user: avatar bulat, status, tombol
│               │   ├── activity_profile_settings.xml # Ganti foto + ganti password
│               │   ├── item_employee.xml          # Item index publik (avatar foto bulat)
│               │   ├── item_admin_employee.xml    # Item admin (avatar foto bulat)
│               │   ├── item_history.xml / item_department_header.xml / item_stat.xml
│               │   └── activity_history.xml / activity_login.xml / activity_admin.xml
│               └── values/
│                   └── colors.xml                 # Design tokens (Monochrome & Badges)
```

---

## 🌐 API & Network Infrastructure

The app queries API endpoints with automatic failover fallback:
1. Primary: `http://192.168.1.37:9790/api_public.php`
2. Secondary: `http://100.102.13.11:9790/api_public.php`

Expected Response JSON:
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

### Alur
- **Halaman profil user normal** (`EmployeeActivity`): avatar bulat menampilkan foto asli; **ketuk avatar** → buka `ProfileSettingsActivity`.
- **`ProfileSettingsActivity`**: tombol **Ganti Foto** (pilih dari galeri → dimampatkan ke ≤2400px → upload multipart → tampil langsung), form **ganti password** (lama / baru / konfirmasi + pesan error), tombol kembali.
- **Index Publik** (`MainActivity`/`AbsensiAdapter`) & **Dashboard Admin** (`AdminAdapter`): avatar menampilkan **foto karyawan bulat** (di-cache per emp_code, dimuat async, `itemView.post` saat selesai — jangan panggil `notifyItemChanged` dari thread background). Fallback inisial lingkaran berwarna.

### Konvensi
- Semua avatar = **circle** (`drawable/bg_avatar_circle.xml`), diterapkan `background` oval + `clipToOutline="true"` **langsung pada `ImageView`/`TextView`** (bukan parent FrameLayout) — ini yang terbukti menghasilkan lingkaran yang pas.
- Untuk foto di adapter: selalu `photoCache[synchronizedMap]` + `Thread` + `itemView.post {}` (hindari `CalledFromWrongThreadException`).
