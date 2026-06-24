import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "io.github.adamwbrown.rtf"
version = "0.1.0-SNAPSHOT"

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

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
    watchosArm64()
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
