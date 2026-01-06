# Android ProGuard/R8 rules
# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Kotlinx Serialization
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class **$Companion {
    public kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ** {
    public static ** Companion;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-dontwarn kotlinx.serialization.**

# Ktor (OkHttp engine + serialization providers)
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep class io.ktor.serialization.** { *; }
-dontwarn io.ktor.**

# SQLDelight
-keep class com.squareup.sqldelight.** { *; }
-dontwarn com.squareup.sqldelight.**

# Bundled SQLite driver (Room KMP)
-keep class androidx.sqlite.driver.bundled.** { *; }
-dontwarn androidx.sqlite.driver.bundled.**

# Sentry
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**
