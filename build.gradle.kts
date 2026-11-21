// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP's built-in Kotlin brings an older Kotlin Gradle plugin; this classpath entry raises it.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    // Turned off at setup because existing code fails them (ktlint has no baseline).
    val ktlintRulesOff =
        mapOf(
            "ktlint_standard_no-wildcard-imports" to "disabled",
            "ktlint_standard_import-ordering" to "disabled",
            "ktlint_standard_final-newline" to "disabled"
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
