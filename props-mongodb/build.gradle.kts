plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":props-core"))

    implementation("org.mongodb:mongodb-driver-sync:4.5.1")
    testFixturesImplementation("org.mongodb:mongodb-driver-sync:4.5.1")
    intTestImplementation(testFixtures(project(":props-mongodb")))

    intTestImplementation(platform("org.testcontainers:testcontainers-bom:1.16.3"))
    intTestImplementation("org.testcontainers:mongodb")
    intTestImplementation("org.testcontainers:junit-jupiter")
    intTestImplementation("ch.qos.logback:logback-core:1.2.11")
}

// specify the Testcontainers MongoDB container image version to use in integration tests
tasks.named<Test>("integrationTest") {
    val version = rootProject.testcontainers.versions.mongo.get()

    systemProperty(
        "testcontainers:mongo",
        "mongo:${version}"
    )
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
                description.set("MongoDB-backed source for sh.props:props-core")
                url.set("https://github.com/props-sh/props/tree/main/props-mongodb")
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
