import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set(project.name)
        description.set("Idiomatic Kotlin Multiplatform RTF reader and writer — a port of RTFParserKit.")
        url.set("https://github.com/Darkrock-Studios/RtfParserKmp")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("wavesonics")
                name.set("Adam Brown")
                url.set("https://github.com/Wavesonics")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/Darkrock-Studios/RtfParserKmp.git")
            developerConnection.set("scm:git:ssh://github.com/Darkrock-Studios/RtfParserKmp.git")
            url.set("https://github.com/Darkrock-Studios/RtfParserKmp")
        }
    }
}
