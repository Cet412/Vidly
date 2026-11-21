# Vidly — Android Video Downloader

<div align="center">

![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09-4285F4?logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

**Unduh video & foto dari Instagram, TikTok, dan YouTube langsung dari share menu.**

</div>

---

## ✨ Fitur

- **Share Intent** — Bagikan link dari Instagram/TikTok/YouTube → Vidly langsung unduh di background
- **Tanpa login** — Tidak perlu akun atau database cloud
- **Riwayat lokal** — Semua unduhan tersimpan di galeri (`Movies/Vidly`, `Pictures/Vidly`)
- **Notifikasi progress** — Lihat progres unduhan dan notifikasi selesai
- **Error handling** — Retry otomatis, pesan error dalam Bahasa Indonesia
- **Rate limit** — 5 unduhan/jam per IP (backend)

## 🏗️ Arsitektur

```
┌─────────────────┐     POST /api/v1/download     ┌─────────────────┐
│  Android App    │ ──────────────────────────────▶ │  Go Backend     │
│  (Kotlin/Compose)│ ◀────────────────────────────── │  (Gin + yt-dlp) │
│                 │      Binary stream + headers    │                 │
└─────────────────┘                                 └─────────────────┘
        │                                                   │
        ▼                                                   ▼
   MediaStore (local)                              EC2 + Nginx + HTTPS
```

## 📱 Download

### APK (Debug/Staging)
Unduh dari [GitHub Actions](https://github.com/Cet412/vidly/actions) → pilih run terbaru → **Artifacts** → `vidly-staging-debug`.

### Release (Signed)
Unduh dari [GitHub Releases](https://github.com/Cet412/vidly/releases) (hanya rilis resmi).

## 🚀 Build Lokal

### Prasyarat
- Android Studio (JBR 17+)
- Android SDK 36
- Gradle 8.9+

### Langkah
```powershell
# 1. Clone repo
git clone https://github.com/Cet412/vidly.git
cd vidly

# 2. Build debug
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleStagingDebug

# 3. Install ke HP
adb install app\build\outputs\apk\staging\debug\app-staging-debug.apk
```

### Build release (signed)
```powershell
# Buat keystore.properties di root project
# storeFile=release.keystore
# storePassword=...
# keyAlias=vidly
# keyPassword=...

.\gradlew.bat assembleProductionRelease
```

## 🔧 Konfigurasi

### Backend URL
Di `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://vidly-staging.ddns.net/\"")
```

### Environment
- **Staging**: `https://vidly-staging.ddns.net/`
- **Production**: (belum tersedia)

## 📂 Struktur Project

```
vidly/
├── app/
│   ├── src/main/
│   │   ├── java/com/vidly/app/
│   │   │   ├── MainActivity.kt          # UI + download flow
│   │   │   ├── data/
│   │   │   │   ├── api/                 # Retrofit client
│   │   │   │   └── repository/          # DownloadRepository
│   │   │   ├── notification/            # NotificationHelper
│   │   │   ├── receiver/                # ShareReceiver
│   │   │   ├── service/                 # DownloadService (background)
│   │   │   └── util/                    # NetworkUtils
│   │   └── res/
│   │       └── drawable/                # ic_download, ic_done
│   └── build.gradle.kts
├── .github/workflows/
│   ├── android-build.yml                # CI: build + test
│   └── android-release.yml              # CD: signed APK + release
├── README.md
└── .gitignore
```

## 🧪 Testing

```powershell
# Unit tests
.\gradlew.bat testStagingDebugUnitTest

# Build semua variant
.\gradlew.bat assembleStagingDebug assembleProductionRelease
```

## 📋 CI/CD

| Workflow | Trigger | Output |
|---|---|---|
| `android-build.yml` | push/PR ke `staging`/`main` | APK debug + test report |
| `android-release.yml` | push ke `main` + tag `v*` | APK release signed + GitHub Release |

### Secrets yang diperlukan
| Secret | Deskripsi |
|---|---|
| `KEYSTORE_BASE64` | Keystore (base64) |
| `KEYSTORE_PASSWORD` | Password keystore |
| `KEY_ALIAS` | Alias key (`vidly`) |
| `KEY_PASSWORD` | Password key |

## 🤝 Kontribusi

1. Fork repo
2. Buat branch feature (`git checkout -b feat/nama-fitur`)
3. Commit perubahan (`git commit -m "feat: deskripsi"`)
4. Push ke branch (`git push origin feat/nama-fitur`)
5. Buat Pull Request ke `staging`

## 📄 Lisensi

MIT License — lihat [LICENSE](LICENSE) untuk detail.

---

<div align="center">

**Dibuat dengan ❤️ untuk komunitas Indonesia**

</div>
