plugins {
    id("rtf.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":rtf-core"))
        }
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain.get().dependsOn(jvmCommonMain)
        androidMain.get().dependsOn(jvmCommonMain)
        val fallbackMain by creating {
            dependsOn(commonMain.get())
        }
        wasmWasiMain.get().dependsOn(fallbackMain)
        linuxMain.get().dependsOn(fallbackMain)
        mingwMain.get().dependsOn(fallbackMain)
        // appleMain carries its own CFString decoder (see src/appleMain).
    }
}
