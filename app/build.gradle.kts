plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.edison.tamirkar"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.edison.tamirkar"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "2.1.0-offline-engine"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
}
