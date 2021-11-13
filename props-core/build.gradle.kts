plugins {
    id("me.champeau.jmh")
    `maven-publish`
}

jmh {
    iterations.set(1)
}

// Publish to GitHub Packages
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/props-sh/props")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            group = project.group as String
            from(components["java"])
        }
    }
}