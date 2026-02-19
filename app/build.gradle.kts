import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    jacoco
}

android {
    namespace = "com.monkopedia.quickcalc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.monkopedia.quickcalc"
        minSdk = 23
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
                "proguard-rules.pro"
            )
            if (providers.gradleProperty("QUICKCALC_RELEASE_STORE_FILE").isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
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

    lint {
        baseline = file("lint-baseline.xml")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(files("libs/arity-2.1.2.jar"))
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.viewpager)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val ktlint by configurations.creating
val detekt by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli)
    detekt(libs.detekt.cli)
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Runs ktlint checks for app module Kotlin sources."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "--relative",
        "src/**/*.kt",
        "build.gradle.kts"
    )
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Formats app module Kotlin sources with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "-F",
        "--relative",
        "src/**/*.kt",
        "build.gradle.kts"
    )
}

tasks.register<JavaExec>("detektCheck") {
    group = "verification"
    description = "Runs detekt on Kotlin production sources."
    classpath = detekt
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    args(
        "--build-upon-default-config",
        "--max-issues",
        "999",
        "--input",
        "src/main/java/com/monkopedia/quickcalc/CalculatorExpressionBuilder.kt," +
            "src/main/java/com/monkopedia/quickcalc/CalculatorExpressionEvaluator.kt," +
            "src/main/java/com/monkopedia/quickcalc/CalculatorExpressionTokenizer.kt",
        "--base-path",
        projectDir.absolutePath
    )
}

dependencyLocking {
    lockAllConfigurations()
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    listOf(
        "roborazzi.test.record",
        "roborazzi.test.compare",
        "roborazzi.test.verify",
        "roborazzi.record.filePathStrategy"
    ).forEach { key ->
        providers.gradleProperty(key).orNull?.let { value ->
            systemProperty(key, value)
        }
    }
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val nonUiCoverageClassPatterns = listOf(
    "com/monkopedia/quickcalc/CalculatorExpressionBuilder.class",
    "com/monkopedia/quickcalc/CalculatorExpressionEvaluator.class",
    "com/monkopedia/quickcalc/CalculatorExpressionTokenizer.class"
)

val nonUiClassDirectories = layout.buildDirectory
    .dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    .map { classesDir ->
        fileTree(classesDir) {
            include(nonUiCoverageClassPatterns)
        }
    }

val nonUiSourceDirectories = files("src/main/java")

val nonUiExecutionData = files(
    layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"),
    layout.buildDirectory.file(
        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    )
)

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
