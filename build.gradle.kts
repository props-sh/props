// https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
plugins {
    `maven-publish`
}

group = "sh.props"
version = project.version

// install git hooks
tasks.register<Exec>("gitHooks") {
    dependsOn("installGitHooks")

    description = "Ensures the project's git hooks are installed and executable"
    commandLine("chmod", "-R", "+x", "./.git/hooks/")
}
tasks.register<Copy>("installGitHooks") {
    description = "Installs the project's git hooks"

    doLast {
        copy {
            from("./githooks/") {
                include("*")
            }
            into("./.git/hooks")
        }
    }
}

