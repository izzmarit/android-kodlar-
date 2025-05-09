plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.kuluckakontrolu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kuluckakontrolu"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        // BuildConfig sınıfının oluşturulmasını etkinleştir
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.code.gson:gson:2.10.1")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // MPAndroidChart for data visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Firebase
    //implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    //implementation("com.google.firebase:firebase-analytics-ktx")
    //implementation("com.google.firebase:firebase-messaging-ktx")
    //implementation("com.google.firebase:firebase-database-ktx")

    // PDF Generation
    implementation("com.itextpdf:itextpdf:5.5.13.3")

    // OTA for ESP32
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Picasso kütüphanesi - Resim yükleme için
    implementation("com.squareup.picasso:picasso:2.8")

    // Firebase Firebase bağımlılıkları aktifleştiriliyor
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // FileProvider kullanımı için
    implementation("androidx.core:core:1.12.0")

    // Android KTX
    implementation("androidx.core:core-ktx:1.12.0")

    // AlertDialog için
    implementation("androidx.appcompat:appcompat:1.6.1")

    // EditText için
    implementation("com.google.android.material:material:1.11.0")
}