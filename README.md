# Secure WOL (Owner-Only Private Wake-on-LAN Android App)

An ultra-secure, private, single-owner Android application built to ensure that no unauthorized person who gains physical access to your phone can turn on your PC.

Designed in strict compliance with the **17 Owner-Only Security Requirements**.

---

## 🛡️ Security Architecture & Requirement Mapping

| # | Requirement | Implementation in Codebase |
|---|---|---|
| **1** | **Mandatory App Authentication** | [`BiometricAuthManager.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/security/BiometricAuthManager.kt) & [`PinCryptoHelper.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/security/PinCryptoHelper.kt) using `PBKDF2WithHmacSHA256` (100k iterations, 256-bit salt, zero hardcoded PINs, constant-time verification). |
| **2** | **Biometric Lock** | Official Android `BiometricPrompt` supporting Fingerprint, Face ID, and Device Credentials with `"Authenticate to continue"` prompt. |
| **3** | **Automatic App Lock** | [`AppLockLifecycleObserver.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/lifecycle/AppLockLifecycleObserver.kt) with configurable timeouts: **Immediately** (default), **30s**, **1m**, **5m**, plus manual *"Lock App Now"*. |
| **4** | **Failed Attempt Protection** | [`LockoutController.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/security/LockoutController.kt) enforcing progressive lockout: 3 fails $\to$ 30s delay; 5 fails $\to$ 2m delay; 10 fails $\to$ 5m delay. Monotonically stored in encrypted storage. |
| **5** | **Device Binding** | [`DeviceBindingManager.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/security/DeviceBindingManager.kt) binds the app to a cryptographically generated random UUID in Keystore on First Launch. Zero reliance on restricted IMEI/MAC/Serials. |
| **6** | **Secure Key Storage** | Android Keystore AES-256-GCM backed [`KeystoreManager.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/security/KeystoreManager.kt) and `EncryptedSharedPreferences`. `android:allowBackup="false"` in manifest. |
| **7** | **Protect PC Configuration** | [`PcRepository.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/data/repository/PcRepository.kt) stores PC configs encrypted; MAC address masked (`00:11:••:••:••:55`) until explicitly unlocked. |
| **8** | **Lock the POWER ON Function** | [`WolDispatcher.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/network/WolDispatcher.kt) strictly validates [`SessionManager`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/security/SessionManager.kt) token. Unauthenticated requests throw `SecurityException` and are blocked. Zero exported intent/widget/notification paths. |
| **9** | **Prevent Accidental Activation** | Non-dismissible confirmation modal dialog (`Turn On PC? [Cancel] [POWER ON]`) + optional secondary biometric verification before packet transmission. |
| **10** | **Local Network Only** | Pure UDP broadcast on local subnet (`255.255.255.255` or subnet broadcast IP, port 9/7). Zero public REST/WebSocket/cloud listeners. |
| **11** | **Do Not Trust Local Network** | Same Wi-Fi $\neq$ authorized. Biometric / PIN authorization is strictly mandatory regardless of network connection state. |
| **12** | **Privacy & Offline First** | Zero ads, zero trackers, zero analytics libraries, zero cloud dependencies. Operates 100% offline. |
| **13** | **Secure Logging** | [`SecureLogger.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/core/security/SecureLogger.kt) automatically sanitizes and redacts MAC addresses, IPs, passwords, and tokens. |
| **14** | **Security Settings Screen** | [`SecuritySettingsScreen.kt`](file:///C:/Users/PC/.gemini/antigravity/scratch/SecureWolApp/app/src/main/java/com/securewol/app/ui/settings/SecuritySettingsScreen.kt) providing toggles for biometrics, auto-lock timeouts, power-on auth, lockout status, device UUID, change PIN, and re-registration. |
| **15** | **Device Re-Registration** | Destructive re-registration flow that wipes all Keystore credentials and stored PCs when transferring ownership. |
| **16** | **Security Threat Model** | Hardened with `FLAG_SECURE` (anti-screenshot/anti-recents task preview), `android:allowBackup="false"`, `android:exported="false"`, R8 ProGuard obfuscation. |
| **17** | **Security Principle** | Clearly documented security boundary and disclaimer in settings screen and codebase. |

---

## 📁 Project Directory Structure

```
SecureWolApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/securewol/app/
│   │   │   │   ├── core/
│   │   │   │   │   ├── security/
│   │   │   │   │   │   ├── KeystoreManager.kt
│   │   │   │   │   │   ├── BiometricAuthManager.kt
│   │   │   │   │   │   ├── PinCryptoHelper.kt
│   │   │   │   │   │   ├── LockoutController.kt
│   │   │   │   │   │   ├── DeviceBindingManager.kt
│   │   │   │   │   │   ├── SessionManager.kt
│   │   │   │   │   │   └── SecureLogger.kt
│   │   │   │   │   ├── network/
│   │   │   │   │   │   ├── WolPacketBuilder.kt
│   │   │   │   │   │   └── WolDispatcher.kt
│   │   │   │   │   └── lifecycle/
│   │   │   │   │       └── AppLockLifecycleObserver.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/ (PcDevice, SecuritySettings, AuthState)
│   │   │   │   │   └── repository/ (PcRepository, SecurityRepository)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/ (Color, Type, Theme)
│   │   │   │   │   ├── setup/ (SetupScreen, SetupViewModel)
│   │   │   │   │   ├── auth/ (AuthScreen, AuthViewModel)
│   │   │   │   │   ├── dashboard/ (DashboardScreen, DashboardViewModel)
│   │   │   │   │   ├── pcedit/ (PcEditScreen, PcEditViewModel)
│   │   │   │   │   ├── settings/ (SecuritySettingsScreen, SecuritySettingsViewModel)
│   │   │   │   │   └── navigation/ (AppNavigation.kt)
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── SecureWolApplication.kt
│   │   │   └── res/ (strings, themes, xml/data_extraction_rules)
│   │   └── test/java/com/securewol/app/
│   │       ├── WolPacketBuilderTest.kt
│   │       ├── LockoutControllerTest.kt
│   │       └── SessionManagerTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 How to Open and Build in Android Studio

1. Open Android Studio.
2. Select **Open** and choose the directory `C:\Users\PC\.gemini\antigravity\scratch\SecureWolApp`.
3. Let Gradle sync dependencies (Jetpack Compose, AndroidX Biometrics, AndroidX Security Crypto).
4. Run on an Android device or emulator with API level 26+ (Android 8.0 - 14+).

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - Copyright (c) 2026 Wan Muhammad Nur Iman.

