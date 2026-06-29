plugins {
    // Applied (not used) at the root so Dokka can load KotlinBasePlugin across the modules,
    // which apply Kotlin via the build-logic convention plugins rather than the root.
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.dokka)
}

dependencies {
    dokka(project(":rtf-core"))
    dokka(project(":rtf-reader"))
    dokka(project(":rtf-writer"))
    dokka(project(":rtf-io-kotlinx"))
    dokka(project(":rtf-io-okio"))
    dokka(project(":rtf-compose"))
}

dokka {
    moduleName.set("RtfParserKmp")
    dokkaPublications.html {
        outputDirectory.set(rootDir.resolve("docs/api"))
    }
}

tasks.register("updateDocs") {
    group = "documentation"
    description = "Generates the aggregated API docs into docs/api."
    dependsOn("dokkaGenerate")
}
