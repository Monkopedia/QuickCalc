plugins {
    alias(libs.plugins.android.application)
    jacoco
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(files("libs/arity-2.1.2.jar"))
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.viewpager)

    testImplementation(libs.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

dependencyLocking {
    lockAllConfigurations()
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val nonUiCoverageClassPatterns = listOf(
    "com/android/calculator2/CalculatorExpressionBuilder.class",
    "com/android/calculator2/CalculatorExpressionEvaluator.class",
    "com/android/calculator2/CalculatorExpressionTokenizer.class",
)

val nonUiClassDirectories = layout.buildDirectory
    .dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    .map { classesDir ->
        fileTree(classesDir) {
            include(nonUiCoverageClassPatterns)
        }
    }

val nonUiSourceDirectories = files("src/main/java")

val nonUiExecutionData = layout.buildDirectory.asFileTree.matching {
    include("jacoco/testDebugUnitTest.exec")
    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    include("**/testDebugUnitTest.exec")
}

val jacocoNonUiTestReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Generates JaCoCo report for non-UI calculator engine classes."
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(nonUiClassDirectories)
    sourceDirectories.setFrom(nonUiSourceDirectories)
    executionData.setFrom(nonUiExecutionData)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoNonUiCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    group = "verification"
    description = "Enforces non-UI line/branch coverage minimums."
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(nonUiClassDirectories)
    sourceDirectories.setFrom(nonUiSourceDirectories)
    executionData.setFrom(nonUiExecutionData)

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(jacocoNonUiCoverageVerification)
}
