plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mebiad.checkpilot"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mebiad.checkpilot"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
        // These depend on upstream releases, not on this code, so they would fail the check without a code change.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

dependencyLocking {
    lockAllConfigurations()
}

// Android Lint's own runtime is build tooling, pinned through AGP, and never ships in the app.
// Leaving it unlocked keeps the dependency scan on the app's own dependencies.
configurations.matching { it.name == "androidLintTool" }.configureEach {
    resolutionStrategy.deactivateDependencyLocking()
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}