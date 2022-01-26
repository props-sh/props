rootProject.name = "props"

include("props-core")
include("props-aws")
include("props-mongodb")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.diffplug.spotless").version("5.17.1")
        id("net.ltgt.errorprone").version("2.0.2")
        id("me.champeau.jmh").version("0.6.6")
        id("org.sonarqube").version("3.3")
        id("pmd").version("6.41.0")
        id("jacoco").version("0.8.7")
    }
}

// For when we will need to define constraints in our dependency tree
// https://docs.gradle.org/current/userguide/dependency_constraints.html#dependency-constraints

// https://docs.gradle.org/current/userguide/platforms.html#header
// https://docs.gradle.org/current/userguide/dependency_version_alignment.html#header
// https://docs.gradle.org/current/userguide/single_versions.html#header
enableFeaturePreview("VERSION_CATALOGS")
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("checkstyle", "9.0")
            version("junit", "5.8.1")

            alias("errorprone").to("com.google.errorprone", "error_prone_core")
                .version("2.9.0")
            alias("nullaway").to("com.uber.nullaway", "nullaway").version("0.9.2")

            alias("junit-jupiter-api").to("org.junit.jupiter", "junit-jupiter-api")
                .versionRef("junit")
            alias("junit-jupiter-engine").to("org.junit.jupiter", "junit-jupiter-engine")
                .versionRef("junit")
            alias("junit-jupiter-params").to("org.junit.jupiter", "junit-jupiter-params")
                .versionRef("junit")
            alias("org-hamcrest-core").to("org.hamcrest:hamcrest-core:2.2")
            alias("org-mockito-core").to("org.mockito:mockito-core:4.3.1")
            alias("org-awaitility-awaitility").to("org.awaitility:awaitility:4.1.1")

            alias("guava").to("com.google.guava:guava:31.0.1-jre")
        }
        create("testcontainers") {
            version("mongo", "5.0.5-focal")
        }
    }
}
