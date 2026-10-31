# Proguard rules for NOVA Player Pro
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-dontwarn com.hierynomus.smbj.**
-keep class com.hierynomus.smbj.** { *; }
