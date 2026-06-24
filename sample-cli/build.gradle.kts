plugins {
    id("rtf.native-app")
}

kotlin {
    val nativeTargets = listOf(
        mingwX64(),
        linuxX64(),
        macosX64(),
        macosArm64(),
    )

    nativeTargets.forEach { target ->
        target.binaries.executable {
            entryPoint = "main"
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(project(":rtf-reader"))
            implementation(project(":rtf-writer"))
            implementation(project(":rtf-io-kotlinx"))
            implementation(libs.kotlinx.io.core)
        }
    }
}
