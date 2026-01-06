# Android ProGuard/R8 rules
# Keep Koin, Room, and Serialization metadata
-keep class org.koin.** { *; }
-dontwarn org.koin.**

-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
