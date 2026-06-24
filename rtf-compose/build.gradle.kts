plugins {
    id("rtf.compose")
    id("rtf.publish")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":rtf-core"))
            api(project(":rtf-reader"))
            api(libs.compose.ui.text)
        }
    }
}
