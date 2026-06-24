plugins {
    id("rtf.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":rtf-core"))
            api(libs.kotlinx.io.core)
        }
        commonTest.dependencies {
            implementation(project(":rtf-reader"))
        }
    }
}
