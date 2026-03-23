// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // `apply false` means the plugin is declared here but NOT applied to the root project.
    // It will be applied only to the :app module in its own build.gradle.kts.
    alias(libs.plugins.kotlin.serialization) apply false
}