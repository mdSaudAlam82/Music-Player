# Retrofit specific rules
-keepattributes Signature, InnerClasses, AnnotationDefault
-keep class retrofit2.** { *; }
-keep @interface retrofit2.http.** { *; }

# DTO models ko specific rakha hai taaki API responses sahi se parse hon aur warning na aaye
-keep class com.example.musicplayer.data.remote.dto.** { <fields>; <methods>; }
-keep class com.example.musicplayer.data.remote.api.** { <methods>; }

# Hilt & Dependency Injection
-keep class dagger.hilt.** { *; }

# Media3 & ExoPlayer (Music bajne ke liye zaroori)
-keep class androidx.media3.** { *; }

# Coil (Album art load karne ke liye)
-keep class coil.** { *; }

# Room Database (Agar aap playlists save kar rahe hain)
-keep class com.example.musicplayer.data.local.** { *; }