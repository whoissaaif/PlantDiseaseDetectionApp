import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.plantdiseasedetectionapp"
    // FIX: Updated compileSdk from 35 to 36
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.plantdiseasedetectionapp"
        minSdk = 26
        // FIX: Updated targetSdk from 35 to 36 to match compileSdk
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // This enables support for newer Java APIs on older Android versions
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    aaptOptions {
        noCompress += "tflite"
    }
}

dependencies {
    // This is the dependency that provides the implementations for desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")
    // Switch to lower-level TFLite libraries
    implementation("org.tensorflow:tensorflow-lite:2.15.0")

    // The exclusions you already have are good practice, so we'll keep them.
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("com.google.guava:guava:31.0.1-android") // Use Android-compatible Guava

    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4") {
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
