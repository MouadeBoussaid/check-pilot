// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    // Turned off at setup because existing code fails them (ktlint has no baseline).
    val ktlintRulesOff =
        mapOf(
            "ktlint_standard_no-wildcard-imports" to "disabled",
            "ktlint_standard_import-ordering" to "disabled",
            "ktlint_standard_final-newline" to "disabled",
        )
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintRulesOff)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintRulesOff)
    }
}
