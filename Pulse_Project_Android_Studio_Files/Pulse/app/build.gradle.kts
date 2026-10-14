plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Reads app/google-services.json for Firebase.
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pulse.budget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pulse.budget"
        // Android 8.0+ so the app installs on most phones.
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Lets unit tests call code that touches android.util.Log without crashing on the JVM.
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // DataStore: dark/light theme setting.
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil: photo previews.
    implementation("io.coil-kt:coil-compose:2.6.0")

    // EXIF: keeps uploaded photos upright.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Firebase Authentication + Cloud Firestore (BoM 33.7.0 matches Kotlin 1.9.24).
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // .await() for Firebase Tasks.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
}
