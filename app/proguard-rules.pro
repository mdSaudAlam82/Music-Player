# ==========================================
# MUSIC PLAYER PROGUARD RULES
# ==========================================

# 1. Models & GSON (Network Data bachaane ke liye)
-keep class com.example.musicplayer.data.remote.dto.** { *; }
-keep class com.example.musicplayer.domain.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.Unsafe
-keep class com.google.gson.** { *; }

# 2. Retrofit (API calls theek rahein)
-keep,allowobfuscation,allowshrinking interface retrofit2.http.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 3. Room Database (Database bachao)
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.musicplayer.data.local.entity.** { *; }
-keep class com.example.musicplayer.data.local.dao.** { *; }

# 4. Hilt / Dagger (DI system)
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep,allowobfuscation,allowshrinking @interface dagger.hilt.*
-keep,allowobfuscation,allowshrinking @interface javax.inject.*

# 5. ExoPlayer / Media3
-keep class androidx.media3.** { *; }

# 6. Coil & Palette (Images aur colors ke liye)
-keep class coil.** { *; }
-keep class androidx.palette.** { *; }

# 7. Coroutines (Background tasks)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}