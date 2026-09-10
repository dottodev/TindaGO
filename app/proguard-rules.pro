# Keep Room, Compose, Navigation, data classes
-keep class com.tindahan.tracker.data.local.entities.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keepattributes Signature, InnerClasses, EnclosingMethod
# Keep BuildConfig for backup version checks
-keep class com.tindahan.tracker.BuildConfig { *; }
# Coroutines / Flow
-dontwarn kotlinx.coroutines.**
# Material3 / Compose are handled by default R8 rules
