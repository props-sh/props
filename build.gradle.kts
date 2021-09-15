// https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
plugins {
    `maven-publish`
    id("com.star-zero.gradle.githook")
}

group = "sh.props"
version = project.version
