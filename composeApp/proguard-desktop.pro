# Desktop ProGuard rules
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

# Keep Ktor serialization providers and ServiceLoader metadata
-keep class io.ktor.serialization.kotlinx.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep class io.ktor.serialization.** { *; }
-keep resourcefiles META-INF/services/**

-dontnote
-ignorewarnings
