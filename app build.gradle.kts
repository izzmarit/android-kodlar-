plugins {
    id 'com.android.application'
}

android {
    namespace 'com.kulucka.mk'
    compileSdk 34

    defaultConfig {
        applicationId "com.kulucka.mk"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "5.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

        // Retrofit ve network işlemleri için
        buildConfigField "String", "ESP32_BASE_URL", "\"http://192.168.4.1:80/\""
    }

    buildTypes {
        debug {
            minifyEnabled false
            debuggable true
            buildConfigField "Boolean", "ENABLE_LOGGING", "true"
        }

        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            buildConfigField "Boolean", "ENABLE_LOGGING", "false"
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding true
        buildConfig true
    }
}

dependencies {
    // AndroidX Core
    implementation 'androidx.core:core:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'

    // Material Design
    implementation 'com.google.android.material:material:1.11.0'

    // Lifecycle Components
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime:2.7.0'

    // Navigation
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'

    // LocalBroadcastManager
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'

    // Retrofit for API calls
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Gson for JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'

    // Network debugging (optional)
    debugImplementation 'com.facebook.stetho:stetho:1.6.0'
    debugImplementation 'com.facebook.stetho:stetho-okhttp3:1.6.0'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}