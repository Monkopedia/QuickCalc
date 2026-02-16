plugins {
    alias(libs.plugins.android.application) apply false
    base
}

tasks.register<Exec>("checkLicenseHeaders") {
    group = "verification"
    description = "Validates required license artifacts and source license headers."
    commandLine("./scripts/check_license_headers.sh")
}

tasks.named("check") {
    dependsOn("checkLicenseHeaders")
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
