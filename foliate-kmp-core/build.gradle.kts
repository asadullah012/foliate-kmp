plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.central.publish)
    alias(libs.plugins.dokka)
}

kotlin {
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()

    android {
        namespace = "io.github.asadullah012.foliate"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}

        // The Android KMP library plugin keeps resource processing off by default.
        // Without this flag the AAR contains no assets, and the foliate-js engine
        // never reaches a consumer application.
        androidResources {
            enable = true
        }

        // The AAR carries these rules, so a consumer application that uses R8
        // needs no extra configuration.
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.file("consumer-rules.pro")
        }
    }

    // Compose Multiplatform publishes no iosX64 artifacts, so an Intel simulator
    // target is not possible. Apple silicon simulators use iosSimulatorArm64.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        }

        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.webkit)
        }
    }
}

// Pin the generated resource package. The default value derives from the Maven group
// and the module name, so it changes whenever the coordinates change. The platform
// bridges read the engine files through this class, and the name must stay stable.
compose.resources {
    packageOfResClass = "io.github.asadullah012.foliate.resources"
    generateResClass = always
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    // The release workflow supplies the key through ORG_GRADLE_PROJECT_signingInMemoryKey.
    // Test for that property too, or the upload carries no signature and Maven
    // Central rejects it.
    if (
        project.hasProperty("signingInMemoryKey") ||
        project.hasProperty("signing.keyId") ||
        project.hasProperty("signing.gnupg.keyName")
    ) {
        signAllPublications()
    }
}
