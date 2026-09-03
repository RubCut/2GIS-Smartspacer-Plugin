plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rubcut.gis2smartspacer"
    // Material 1.14 depends on AndroidX lines that require API 36.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rubcut.gis2smartspacer"
        minSdk = 29
        targetSdk = 35
        versionCode = 8
        versionName = "2.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    // Kotlin 2.2 removed the old android.kotlinOptions DSL.
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("com.kieronquinn.smartspacer:sdk-plugin:1.1")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    // Material 3 Expressive: expressive themes, large flexible app bar,
    // expressive buttons, sliders and wavy progress indicators.
    implementation("com.google.android.material:material:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
}
