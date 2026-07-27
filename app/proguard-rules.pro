
# Guardexa application rules.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Hilt/Dagger.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent
-dontwarn dagger.hilt.**

# Room.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# MediaPipe.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# TensorFlow Lite.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep model and serialized DTO field names where reflection/metadata may need them.
-keepclassmembers class com.guardexa.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Android components referenced from the manifest.
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.admin.DeviceAdminReceiver
-keep public class * extends androidx.activity.ComponentActivity

# Do not log through source-retained debug helpers in release.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
