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
        versionCode = 7
        versionName = "2.0.0"

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
            // Include native debug symbols in the App Bundle so Google Play Console can
            // symbolicate native crash stack traces (e.g. from Compose's native libraries).
            // "FULL"         → unstripped .so files (largest, most detailed — recommended for
            //                  Play Console crash analysis)
            // "SYMBOL_TABLE" → stripped symbols only (smaller upload, less detail)
            // "NONE"         → default; triggers the Play Console warning we are fixing
            ndk {
                debugSymbolLevel = "FULL"
            }
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

// ── Mutation testing (PIT) ────────────────────────────────────────────────────
// PIT works by modifying ("mutating") the compiled bytecode — e.g. changing a
// `+` to `-`, flipping a boolean — and then re-running the unit tests.
// If a mutated build still passes all tests, the mutant "survived", which means
// our tests are not checking that particular behaviour.
//
// Run:   ./gradlew pitest
// Report: app/build/reports/pitest/index.html
//
// The build FAILS if the mutation score drops below 80 % (--mutationThreshold).
// Surviving mutants must be addressed by writing new tests.
//
// WHY NOT the info.solidsoft.pitest Gradle plugin:
//   The plugin's extension (PitestPluginExtension) is registered inside the
//   plugin's own afterEvaluate hook. Under AGP 9.x, this hook never fires
//   (the extension stays null regardless of how the plugin is applied). The
//   root cause is an AGP / pitest-plugin afterEvaluate ordering conflict.
//   Running PIT via JavaExec bypasses the plugin entirely and is fully
//   compatible with any Gradle/AGP version.

// A dedicated configuration to hold PIT's own JARs.
// These are tooling, NOT app dependencies — they don't end up in the APK.
val pitestEngine: Configuration by configurations.creating {
    isCanBeConsumed = false  // we only resolve this, never publish it
    isCanBeResolved = true
    isTransitive = true      // pull in pitest's own transitive deps (pitest-core, etc.)
}

// afterEvaluate is needed so that AGP has finished registering the
// testDebugUnitTest task (and its classpath) before we read them.
afterEvaluate {

    // The test task built by AGP for JVM unit tests (runs on the local JVM, not
    // a device). Its .classpath includes: production classes, test classes,
    // android.jar stubs, all runtime dependencies — everything PIT needs.
    val testTask = tasks.named<Test>("testDebugUnitTest").get()

    // Where AGP places the compiled Kotlin bytecode for the debug variant.
    // AGP 9.x stores classes under intermediates/built_in_kotlinc/ (not tmp/kotlin-classes/).
    val kotlinClasses = layout.buildDirectory
        .dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
        .get().asFile

    tasks.register<JavaExec>("pitest") {
        group = "Verification"
        description = "Run PIT mutation testing. Report: build/reports/pitest/index.html"

        // Compile production Kotlin classes and their unit-test counterpart first.
        dependsOn("compileDebugKotlin", "compileDebugUnitTestKotlin")

        // PIT itself runs as a separate JVM process.
        // The JVM's classpath (pitestEngine) holds PIT's own engine JARs.
        // The application code is passed separately via --classPath below.
        classpath = pitestEngine
        mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")

        val reportDir = layout.buildDirectory.dir("reports/pitest").get().asFile

        // doFirst runs just before the JavaExec process is launched.
        // We resolve the test classpath here (at execution time) to avoid
        // forcing dependency resolution during the configuration phase.
        doFirst {
            reportDir.mkdirs()

            // Combine the AGP-managed test classpath with the compiled Kotlin
            // classes dir. testTask.classpath already contains almost everything;
            // adding kotlinClasses ensures the un-transformed .class files are
            // on the path for PIT to analyse.
            //
            // IMPORTANT: PIT's --classPath expects COMMA-separated entries, not
            // the OS path separator (':' on Linux). Using asPath would produce a
            // single colon-separated string that PIT treats as one giant path and
            // finds no classes inside.
            val fullClasspath = (testTask.classpath + files(kotlinClasses))
                .files.joinToString(",") { it.absolutePath }

            args(
                // Output directory for HTML / XML reports.
                "--reportDir", reportDir.absolutePath,

                // Only mutate classes in our own package.
                // PIT matches against the binary class name, e.g.
                // "fr.mandarine.tarotcounter.GameModels".
                "--targetClasses", "fr.mandarine.tarotcounter.*",

                // Only use our own test classes to detect mutations.
                "--targetTests", "fr.mandarine.tarotcounter.*",

                // Source directories are used by PIT to show annotated source code
                // in the HTML report alongside each mutation.
                "--sourceDirs", file("src/main/kotlin").absolutePath,

                // The full classpath PIT uses to load production classes (to mutate)
                // and run the test classes (to detect mutations).
                "--classPath", fullClasspath,

                // Produce a human-readable HTML report and a CI-parseable XML report.
                "--outputFormats", "HTML,XML",

                // Parallelism: 2 threads run independent mutation analysis in parallel.
                "--threads", "2",

                // Maximum verbosity for debugging; remove once confirmed working.
                "--verbosity", "VERBOSE",

                // Quality gate: if fewer than 80 % of mutants are killed, PIT exits
                // with a non-zero status and Gradle marks the task as FAILED.
                // Only lower this temporarily while bootstrapping coverage; keep it
                // at 80 for any code that merges to main.
                "--mutationThreshold", "80",

                // Exclude classes that either (a) have no unit-test coverage, (b) are
                // generated by the Kotlin/Android toolchain, or (c) are test helpers.
                // Categories:
                //   - Compose UI composables: only tested via instrumented tests, not unit tests
                //   - Generated code: serializers, R class, BuildConfig, ComposableSingletons
                //   - Android-specific I/O: GameStorage uses DataStore which can't run on JVM
                //   - Test files: PIT must not mutate the test code itself
                "--excludedClasses",
                // Auto-generated Android resources & build info
                "fr.mandarine.tarotcounter.BuildConfig," +
                "fr.mandarine.tarotcounter.R," +
                // Compose screen composables (only exercised by instrumented tests).
                // The trailing * is required to also exclude anonymous lambda classes
                // that Kotlin generates for composable lambdas (e.g. GameScreenKt$GameScreen$2$1).
                "fr.mandarine.tarotcounter.GameScreenKt*," +
                "fr.mandarine.tarotcounter.LandingScreenKt*," +
                "fr.mandarine.tarotcounter.FinalScoreScreenKt*," +
                "fr.mandarine.tarotcounter.ScoreHistoryScreenKt*," +
                "fr.mandarine.tarotcounter.UiComponentsKt*," +
                "fr.mandarine.tarotcounter.ScreenHeaderKt*," +
                "fr.mandarine.tarotcounter.SettingsScreenKt*," +
                "fr.mandarine.tarotcounter.RulesScreenKt*," +
                // BonusRow is a composable compiled to its own class (lives in UiComponents.kt)
                "fr.mandarine.tarotcounter.BonusRow*," +
                // Compose compiler-generated singletons
                "fr.mandarine.tarotcounter.ComposableSingletons*," +
                // Android Activity and DataStore storage (can't run on JVM)
                "fr.mandarine.tarotcounter.MainActivity*," +
                "fr.mandarine.tarotcounter.GameStorage*," +
                // Kotlin serialization plugin-generated serializer classes
                "fr.mandarine.tarotcounter.*\$\$serializer," +
                "fr.mandarine.tarotcounter.*\$serializer," +
                // Material theme declarations — pure style constants, no logic
                "fr.mandarine.tarotcounter.ui.theme.*," +
                // AppStrings is a pure data-holder (a data class with 60+ val fields and
                // lambda fields).  The generated equals / hashCode / copy / componentN
                // methods are never called by unit tests and produce 45 no-coverage mutants.
                // The logic inside the lambda fields (roundsPlayed, playerFallback, …) is
                // tested through AppLocaleTest via appStrings(locale).field(…) calls.
                "fr.mandarine.tarotcounter.AppStrings," +
                // Test classes and test helpers must not be mutated.
                // The trailing * after Test is required to also exclude the anonymous
                // inner classes that Kotlin generates for coroutine / lambda bodies
                // inside test methods (e.g. GameViewModelTest$someMethod$1).
                "fr.mandarine.tarotcounter.*Test*," +
                "fr.mandarine.tarotcounter.FakeGameStorage*"
            )
        }
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

    // ── PIT engine (mutation testing tooling only — not shipped in the APK) ───
    // pitest-command-line is the entry-point JAR that drives mutation analysis.
    // It pulls in pitest-core (the mutation engine) transitively.
    // Version kept in sync with the version documented in docs/mutation-testing.md.
    pitestEngine("org.pitest:pitest-command-line:1.17.3")
}
