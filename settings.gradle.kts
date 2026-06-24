pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "rtfparserkmp"

include(":rtf-core")
include(":rtf-reader")
include(":rtf-writer")
include(":rtf-io-kotlinx")
include(":rtf-io-okio")
include(":sample-cli")
