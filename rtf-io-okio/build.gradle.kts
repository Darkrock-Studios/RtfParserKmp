plugins {
    id("rtf.multiplatform")
    id("rtf.publish")
    alias(libs.plugins.dokka)
}

dokka {
    moduleName.set("rtf-io-okio")
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
            api(libs.okio)
        }
        commonTest.dependencies {
            implementation(project(":rtf-reader"))
        }
    }
}
