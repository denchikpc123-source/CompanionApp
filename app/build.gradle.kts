plugins {
    id("com.android.application") version "8.3.0"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.example.companion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.companion"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Библиотека для POST-запросов в LM Studio
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
