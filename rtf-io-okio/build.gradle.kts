plugins {
    id("rtf.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":rtf-core"))
            api(libs.okio)
        }
        commonTest.dependencies {
            implementation(project(":rtf-reader"))
        }
    }
}
