plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { 
compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "com.harmonyvoice.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.harmonyvoice.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Moteur de traitement audio pour les harmonies
    implementation("com.tianscar.soundtouch:soundtouch-jni-android:1.1.1")
    implementation("com.tianscar.soundtouch:soundtouch-jni-core:1.1.1")
}
