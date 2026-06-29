plugins {
    id("rtf.compose")
    id("rtf.publish")
    alias(libs.plugins.dokka)
}

dokka {
    moduleName.set("rtf-compose")
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
        sourceLink {
            localDirectory.set(rootDir)
            remoteUrl("https://github.com/Darkrock-Studios/RtfParserKmp/blob/main")
            remoteLineSuffix.set("#L")
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":rtf-core"))
            api(project(":rtf-reader"))
            api(project(":rtf-writer"))
            api(libs.compose.ui.text)
        }
    }
}
