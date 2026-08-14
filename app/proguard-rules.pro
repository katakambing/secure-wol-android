# Security App Proguard / R8 Rules

# Prevent sensitive models from having fields stripped if serialized
-keepclassmembers class com.securewol.app.data.model.** { *; }
-keepclassmembers class com.securewol.app.core.security.** { *; }

# Obfuscate internal security logic
-repackageclasses 'com.securewol.app.internal'
-allowaccessmodification

# Strip logging in release builds to enforce zero log leakage
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Android Jetpack Security Crypto
-keep class androidx.security.crypto.** { *; }
