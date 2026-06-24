plugins {
    id("rtf.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":rtf-core"))
        }
        commonTest.dependencies {
            implementation(project(":rtf-reader"))
        }
    }
}
