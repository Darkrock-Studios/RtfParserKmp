import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

group = "com.darkrockstudios"
version = (findProperty("library.version") as String?) ?: "0.1.0-SNAPSHOT"

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    androidLibrary {
        namespace = "com.darkrockstudios.libs.rtfparserkmp." + project.name.replace("-", "")
        compileSdk = 36
        minSdk = 21
        withHostTest { }
    }

    js(IR) {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosX64()
    // watchosArm64 (arm64_32) is intentionally omitted: it is the only 32-bit Apple target, and its
    // differing pointer width prevents commonizing the CFString-based decoder in shared appleMain.
    watchosSimulatorArm64()
    watchosDeviceArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
