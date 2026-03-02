# Retrofit aur OkHttp (Network safety)
-keepattributes Signature, InnerClasses, AnnotationDefault
-keep class retrofit2.** { *; }
-keep @interface retrofit2.http.** { *; }

# Data Models (Inhe mat chhedna warna lyrics aur search results gayab ho jayenge)
-keep class com.example.musicplayer.data.remote.dto.** { *; }
-keep class com.example.musicplayer.data.remote.api.** { *; }

# Hilt aur Dagger (Dependency Injection)
-keep class dagger.hilt.** { *; }
-keep class com.example.musicplayer.di.** { *; }

# Media3 aur ExoPlayer (Music Engine)
-keep class androidx.media3.** { *; }

# Coil (Images loading ke liye)
-keep class coil.** { *; }

# Room Database
-keep class com.example.musicplayer.data.local.** { *; }