plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Generates JSON serialization code for classes annotated with @Serializable.
    alias(libs.plugins.kotlin.serialization)
}

// ── Release signing credentials ───────────────────────────────────────────────
// Read from gradle.properties (local or ~/.gradle/gradle.properties) first,
// then fall back to environment variables so CI pipelines can inject secrets
// without touching any file on disk.
// All four values must be present for the signing config to be registered;
// if any is missing the release build is produced unsigned (safe for local dev).
val releaseKeystoreFile: String? =
    findProperty("RELEASE_KEYSTORE_FILE")?.toString()
        ?: System.getenv("RELEASE_KEYSTORE_FILE")
val releaseKeystorePassword: String? =
    findProperty("RELEASE_KEYSTORE_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? =
    findProperty("RELEASE_KEY_ALIAS")?.toString()
        ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? =
    findProperty("RELEASE_KEY_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_KEY_PASSWORD")

// True only when every credential is available.
val hasSigningConfig = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).none { it.isNullOrBlank() }

android {
    namespace = "fr.mandarine.tarotcounter"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "fr.mandarine.tarotcounter"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Signing configs ───────────────────────────────────────────────────────
    // Only registered when all four credentials are present (see top of file).
    // The "release" config is named so that buildTypes.release can reference it
    // by name via signingConfigs.getByName("release").
    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                // file() resolves the path relative to the module directory (app/).
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            // Wire the signing config when credentials were provided; otherwise
            // the artifact is left unsigned (you must sign it manually before upload).
            signingConfig = signingConfigs.findByName("release")
            // R8 minification: shrinks bytecode by removing unused code and renaming symbols.
            isMinifyEnabled = true
            // Resource shrinking: strips unused drawables, layouts, strings, etc. from the APK.
            // Must be used together with isMinifyEnabled = true.
            isShrinkResources = true
            proguardFiles(
                // Google's optimised baseline rules bundled with AGP (handles most Android cases).
                getDefaultProguardFile("proguard-android-optimize.txt"),
                // Project-specific rules (see app/proguard-rules.pro).
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // Allow Android framework classes (e.g. Application) to be instantiated on the JVM
        // by returning default values (0 / false / null) instead of throwing RuntimeException.
        // Required for GameViewModelTest, which creates Application() without a device.
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // JSON serialization — converts SavedGame / RoundResult to/from strings for storage.
    implementation(libs.kotlinx.serialization.json)
    // DataStore — persists key-value data on the device (replaces SharedPreferences).
    implementation(libs.androidx.datastore.preferences)
    // ViewModel for Compose — lets us call viewModel() inside a Composable function.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}