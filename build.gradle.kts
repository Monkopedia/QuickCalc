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
}
