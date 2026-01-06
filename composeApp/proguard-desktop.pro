# Desktop ProGuard rules
-dontobfuscate
-dontoptimize
-dontshrink
-dontwarn jakarta.servlet.**
-dontwarn jakarta.mail.**
-dontwarn org.graalvm.nativeimage.**
-dontwarn com.oracle.svm.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.codehaus.janino.**
-dontwarn org.codehaus.commons.compiler.**
-dontwarn org.tukaani.xz.**
-dontwarn okhttp3.internal.graal.**
-dontwarn ch.qos.logback.classic.servlet.**
-dontwarn ch.qos.logback.classic.selector.servlet.**
-dontwarn ch.qos.logback.classic.helpers.MDCInsertingServletFilter
-dontwarn ch.qos.logback.core.net.**
-dontwarn org.freedesktop.dbus.**
-dontwarn module-info
-dontwarn io.sentry.**
-keep class io.sentry.** { *; }

# Keep Ktor serialization providers and ServiceLoader metadata
-keep class io.ktor.serialization.kotlinx.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.client.engine.okhttp.OkHttpEngineContainer { *; }
-keep class io.ktor.client.engine.cio.CIOEngineContainer { *; }

# Kotlinx Serialization keep rules
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class **$Companion {
    public kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ** {
    public static ** Companion;
}
-keep @kotlinx.serialization.Serializable class * { *; }

-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Room/DB
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class com.erpnext.pos.data.AppDatabase { *; }
-keep class com.erpnext.pos.data.AppDatabase_Impl { *; }
-keep class com.erpnext.pos.localSource.dao.** { *; }

-keep class com.erpnext.pos.DesktopLogger { *; }

# Auth navigator and OAuth helpers
-keep class com.erpnext.pos.DesktopAuthNavigator { *; }
-keep class com.erpnext.pos.navigation.AuthNavigator { *; }
-keep class com.erpnext.pos.utils.oauth.OAuthCallbackReceiver { *; }

# Bundled SQLite driver (Room KMP)
-keep class androidx.sqlite.driver.bundled.** { *; }
-dontwarn androidx.sqlite.driver.bundled.**

# SQLDelight (JVM)
-keep class com.squareup.sqldelight.** { *; }
-dontwarn com.squareup.sqldelight.**

-dontnote
-ignorewarnings
