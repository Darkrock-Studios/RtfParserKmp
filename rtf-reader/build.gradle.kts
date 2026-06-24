plugins {
    id("rtf.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":rtf-core"))
        }
        val fallbackMain by creating {
            dependsOn(commonMain.get())
        }
        jsMain.get().dependsOn(fallbackMain)
        wasmJsMain.get().dependsOn(fallbackMain)
        wasmWasiMain.get().dependsOn(fallbackMain)
        nativeMain.get().dependsOn(fallbackMain)
    }
}
