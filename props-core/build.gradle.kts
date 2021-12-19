plugins {
    id("me.champeau.jmh")
}

jmh {
    iterations.set(1)
}

// https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:complete_example
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "props-core"
            group = project.group as String
            from(components["java"])

            pom {
                name.set("Props Core")
                description.set("Layered application property management library for Java")
                url.set("https://github.com/props-sh/props")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/props-sh/props/blob/8243ea7c9c27c47be49dec70a3479dcd131ae2b0/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("mihaibojin")
                        name.set("Mihai Bojin")
                        email.set("mihai.bojin@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:ssh://git@github.com:props-sh/props.git")
                    developerConnection.set("scm:git:ssh://git@github.com:props-sh/props.git")
                    url.set("https://github.com/props-sh/props")
                }
            }
        }

    }
}

// conditional signing: https://docs.gradle.org/current/userguide/signing_plugin.html#sec:conditional_signing
signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}