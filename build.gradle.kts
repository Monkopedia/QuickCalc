plugins {
    base
}

fun compareSemanticVersions(actual: String, minimum: String): Int {
    val actualParts = actual.split(Regex("[^0-9]+"))
        .filter { it.isNotEmpty() }
        .map { it.toInt() }
    val minimumParts = minimum.split(Regex("[^0-9]+"))
        .filter { it.isNotEmpty() }
        .map { it.toInt() }
    val maxSize = maxOf(actualParts.size, minimumParts.size)
    for (index in 0 until maxSize) {
        val actualPart = actualParts.getOrElse(index) { 0 }
        val minimumPart = minimumParts.getOrElse(index) { 0 }
        if (actualPart != minimumPart) {
            return actualPart.compareTo(minimumPart)
        }
    }
    return 0
}

fun verifyMinimumVersion(name: String, actual: String, minimum: String) {
    check(compareSemanticVersions(actual, minimum) >= 0) {
        "$name is $actual, but minimum required is $minimum"
    }
}

tasks.register<Exec>("checkLicenseHeaders") {
    group = "verification"
    description = "Validates required license artifacts and source license headers."
    commandLine("./scripts/check_license_headers.sh")
}

tasks.register("verifyMinimumDependencyVersions") {
    group = "verification"
    description = "Ensures Kotlin and Compose minimum versions remain enforced."
    doLast {
        val versionsToml = file("gradle/libs.versions.toml").readText()
        val kotlinVersion = Regex("""kotlinCompose\s*=\s*"([^"]+)"""")
            .find(versionsToml)
            ?.groupValues
            ?.get(1)
            ?: error("Unable to read kotlinCompose version from gradle/libs.versions.toml")
        verifyMinimumVersion("Kotlin plugin version", kotlinVersion, "2.3.0")

        val lockLines = file("app/gradle.lockfile").readLines()
        fun lockedVersionFor(module: String, configuration: String): String {
            val lockLine = lockLines.firstOrNull { line ->
                line.startsWith("$module:") &&
                    line.substringAfter("=").split(",").contains(configuration)
            } ?: error(
                "Missing lockfile entry for $module in configuration $configuration"
            )
            return lockLine.substringAfter("$module:").substringBefore("=")
        }

        val kotlinStdlibVersion = lockedVersionFor(
            module = "org.jetbrains.kotlin:kotlin-stdlib",
            configuration = "debugRuntimeClasspath"
        )
        verifyMinimumVersion(
            "Kotlin stdlib (debugRuntimeClasspath)",
            kotlinStdlibVersion,
            "2.3.0"
        )

        val composeUiVersion = lockedVersionFor(
            module = "androidx.compose.ui:ui",
            configuration = "debugRuntimeClasspath"
        )
        verifyMinimumVersion(
            "Compose UI (debugRuntimeClasspath)",
            composeUiVersion,
            "1.10.0"
        )
    }
}

tasks.named("check") {
    dependsOn("checkLicenseHeaders")
    dependsOn("verifyMinimumDependencyVersions")
    dependsOn(":app:ktlintCheck")
    dependsOn(":app:detektCheck")
}

tasks.register("ktlintCheck") {
    group = "verification"
    description = "Runs ktlint checks for all modules."
    dependsOn(":app:ktlintCheck")
}

tasks.register("ktlintFormat") {
    group = "formatting"
    description = "Formats Kotlin files for all modules."
    dependsOn(":app:ktlintFormat")
}

tasks.register("detektCheck") {
    group = "verification"
    description = "Runs detekt checks for all modules."
    dependsOn(":app:detektCheck")
}
