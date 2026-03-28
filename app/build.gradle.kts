@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.musicplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.musicplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // 👇 OPTIMIZATION: Faltu bhasha (languages) hata di, sirf English rakhi
        resConfigs("en")

        ndk {
            // 👇 OPTIMIZATION: Sirf modern 64-bit architecture rakha hai (App size aur kam hoga)
            abiFilters.addAll(arrayOf("arm64-v8a", "armeabi-v7a"))
            // Agar Play Store par error aaye purane phones ke liye, tabhi "armeabi-v7a" add karna. Par aayega nahi.
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true // Faltu photos/icons remove karega
            isProfileable = false    // Production me iski zaroorat nahi

            // 👇 OPTIMIZATION: Zip alignment app ko aur chhota banata hai
            isZipAlignEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    ksp {
        arg("incremental", "false")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Retrofit (Networking)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Room (Database)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Coroutines
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)

    // Other specific dependencies
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Coil (Image Loading) - Tumne 2 jagah likha tha[cite: 6], maine saaf kar diya
    implementation(libs.coil.compose)
}