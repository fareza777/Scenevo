# Scenevo ProGuard / R8
-keepattributes *Annotation*
-dontwarn javax.annotation.**
-keep class dagger.hilt.** { *; }
-keep class androidx.media3.** { *; }
