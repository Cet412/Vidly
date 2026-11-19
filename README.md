# Vidly Android — CI/CD

## Workflow

| File | Trigger | Hasil |
|------|---------|-------|
| `.github/workflows/android-build.yml` | push/PR ke `staging` atau `main` | test + APK staging debug (artifact, 7 hari) |
| `.github/workflows/android-release.yml` | push ke `main`, atau tag `v*` | test + APK production release tertandatangani (artifact 30 hari; GitHub Release kalau dari tag) |

Keduanya memakai `if: success()` pada langkah upload, jadi APK gagal build atau test merah tidak pernah diunggah.

## Keystore

### 1. Buat keystore (sekali saja, simpan baik-baik)

```powershell
keytool -genkeypair -v -keystore release.keystore -alias vidly `
  -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` ada di JDK yang dipakai Android Studio:
`C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`

**Kehilangan keystore berarti tidak bisa mengirim update ke pemasangan yang sudah ada.** Simpan salinan di tempat aman (password manager / vault), bukan hanya di laptop.

### 2. Encode ke base64 untuk secret GitHub

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Clipboard
```

### 3. Tambahkan secrets di GitHub

Settings → Secrets and variables → Actions → New repository secret:

| Secret | Isi |
|--------|-----|
| `KEYSTORE_BASE64` | hasil base64 dari langkah 2 (satu baris panjang) |
| `KEYSTORE_PASSWORD` | password keystore |
| `KEY_ALIAS` | `vidly` |
| `KEY_PASSWORD` | password key (sama dengan keystore kalau tidak dibedakan) |

### 4. Build lokal dengan keystore

Buat `keystore.properties` di root project (sudah masuk `.gitignore`):

```properties
storeFile=release.keystore
storePassword=...
keyAlias=vidly
keyPassword=...
```

Tanpa file itu, `assembleProductionRelease` tetap jalan tapi menghasilkan APK unsigned. Di CI hal ini tertangkap oleh langkah `Verify APK is signed`, jadi build yang tidak tertandatangani menggagalkan job alih-alih diam-diam terunggah.

## Alur build lokal

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleStagingDebug
```

APK: `app/build/outputs/apk/staging/debug/app-staging-debug.apk`
