# ==============================================================================
# FlyVision Inspector - Production Mobile ProGuard & R8 Optimization Rules
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. General Optimization & Stack Trace Preservation
# ------------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 2. Core Vision & Neuromorphic EMD Engine (Connectome Algorithms)
# ------------------------------------------------------------------------------
# Preserve 64-point depth profiling, 2024 Nature MaleCNS connectome synaptic weights,
# motion parallax calculations, and domain data models
-keep class com.example.domain.** { *; }
-keepclassmembers class com.example.domain.** { *; }

# Surface Defect & Steger Sub-pixel Detector
-keep class com.example.domain.DefectDetector* { *; }
-keepclassmembers class com.example.domain.DefectDetector* { *; }

# ------------------------------------------------------------------------------
# 3. Haltere IMU 6-Axis Sensor Fusion & Calibration
# ------------------------------------------------------------------------------
# Preserve Android SensorEventListener callbacks, zero-bias compensation, and DSP filtering
-keep class com.example.sensor.** { *; }
-keepclassmembers class com.example.sensor.** { *; }

# ------------------------------------------------------------------------------
# 4. Moshi JSON Serialization & Retrofit Gemini API DTOs
# ------------------------------------------------------------------------------
# Preserve Moshi @JsonClass models and KSP generated JsonAdapters
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class *JsonAdapter {
    <init>(...);
    public <methods>;
}
-keep class com.example.api.** { *; }
-keepclassmembers class com.example.api.** { *; }

# Retrofit service interface & HTTP method annotations
-keep interface com.example.api.GeminiApiService { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Network libraries warning suppressions for clean builds
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------------------------
# 5. Room Database & Local History Persistence
# ------------------------------------------------------------------------------
# Preserve Room entities, SQLite schema mapping, and DAO interfaces
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 6. Utilities & Application State
# ------------------------------------------------------------------------------
-keep class com.example.util.** { *; }
-keepclassmembers class com.example.util.** { *; }

# ------------------------------------------------------------------------------
# 7. AndroidX CameraX & Jetpack Compose
# ------------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
