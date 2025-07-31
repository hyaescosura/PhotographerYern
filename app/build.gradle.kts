plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

    // Kotlin serialization plugin for type safe routes and navigation arguments
    kotlin("plugin.serialization") version "2.0.21" // Keep this specific version for the plugin
}

android {
    namespace = "com.example.photagrapheryern"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.photagrapheryern"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            applicationIdSuffix = ".dev"
            isDebuggable = true
            versionNameSuffix = "-dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Apply the Compose BOM FIRST
    implementation(platform(libs.androidx.compose.bom))

    // AndroidX + Compose (referencing from libs.versions.toml where possible)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose UI and Material3 libraries
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // This correctly gets Material3 from the BOM

    // IMPORTANT: Change this line to use the "material" icons extended
    implementation(libs.androidx.material.icons.extended) // <--- THIS IS THE FIX

    // Firebase (using Firebase BOM)
//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebase.analytics)
//    implementation(libs.firebase.crashlytics.ndk)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics-ndk")

    // Jetpack Compose Navigation
    val nav_version = "2.9.2" // Keep local or from libs.versions.toml
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // Kotlin serialization library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // ML Kit
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}