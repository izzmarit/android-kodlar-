// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    ext {
        gradle_version = '8.1.0'
    }

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath "com.android.tools.build:gradle:$gradle_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}