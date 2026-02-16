plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.android.calculator2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.calculator2"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = providers.gradleProperty("QUICKCALC_RELEASE_STORE_FILE")
            if (storeFilePath.isPresent) {
                storeFile = file(storeFilePath.get())
            }
            storePassword = providers.gradleProperty("QUICKCALC_RELEASE_STORE_PASSWORD").orNull
            keyAlias = providers.gradleProperty("QUICKCALC_RELEASE_KEY_ALIAS").orNull
            keyPassword = providers.gradleProperty("QUICKCALC_RELEASE_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (providers.gradleProperty("QUICKCALC_RELEASE_STORE_FILE").isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(files("libs/arity-2.1.2.jar"))
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.viewpager)

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

dependencyLocking {
    lockAllConfigurations()
}
