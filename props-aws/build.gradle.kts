plugins {
    `java-library`
}

dependencies {
    implementation(project(":props-core"))

    implementation(platform("software.amazon.awssdk:bom:2.17.123"))
    implementation("software.amazon.awssdk:secretsmanager")
}

// https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:complete_example
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            group = project.group as String
            from(components["java"])

            // conditional signing: https://docs.gradle.org/current/userguide/signing_plugin.html#sec:conditional_signing
            signing {
                sign(publishing.publications["mavenJava"])
            }

            pom {
                name.set("${project.group}:${project.name}")
                description.set("AWS Secrets Manager backed source for sh.props:props-core")
                url.set("https://github.com/props-sh/props/tree/main/props-aws")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        id.set("mihaibojin")
                        name.set("Mihai Bojin")
                        email.set("mihai.bojin@gmail.com")
                        organizationUrl.set("https://MihaiBojin.com")
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
