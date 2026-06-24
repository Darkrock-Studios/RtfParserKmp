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
        namespace = "com.darkrockstudios.libs.rtfparserkmp.compose"
        compileSdk = 36
        minSdk = 21
        withHostTest { }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Compose ui-text cannot initialize in the wasmJs test runtimes (Node and headless browser), so
// wasmJs here is a compile/publish-only target; the common logic is covered by jvmTest and the Apple
// test targets.
tasks.matching { it.name == "wasmJsNodeTest" || it.name == "wasmJsBrowserTest" }.configureEach {
    enabled = false
}
