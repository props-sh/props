import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    `java-test-fixtures`
    idea
    checkstyle
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    `maven-publish`
}

group = project.group
version = project.version

buildscript {
    repositories {
        // uncomment if you need to use the local Maven cache
        // mavenLocal()
        mavenCentral()
    }
    dependencies {
        "classpath"(
            group = "com.google.googlejavaformat",
            name = "google-java-format",
            version = "1.13.0"
        )
    }
    allprojects {
        repositories {
            // uncomment if you need to use the local Maven cache
            // mavenLocal()
        }
    }
}

subprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // Java configuration
    apply(plugin = "java-library")
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_11.toString()))
        }
        withJavadocJar()
        withSourcesJar()
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(11)
        options.isIncremental = true
        options.isFork = true
        options.isFailOnError = true
    }
    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }

    // Tests
    tasks.getByName<Test>("test") {
        // https://docs.gradle.org/7.2/userguide/java_testing.html#sec:test_execution
        useJUnitPlatform()
        maxHeapSize = "1G"
    }
    dependencies {
        testImplementation(rootProject.libs.junit.jupiter.api)
        testImplementation(rootProject.libs.org.hamcrest.core)
        testImplementation(rootProject.libs.org.mockito.core)
        testImplementation(rootProject.libs.org.awaitility.awaitility)

        testRuntimeOnly(rootProject.libs.junit.jupiter.engine)
        testCompileOnly(rootProject.libs.junit.jupiter.params)
    }

    // Test Fixtures
    // https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures
    apply(plugin = "java-test-fixtures")
    dependencies {
        // If test fixtures are needed as deps, see:
        // https://docs.gradle.org/current/userguide/java_testing.html#consuming_test_fixtures_of_another_project

        testFixturesImplementation(rootProject.libs.junit.jupiter.api)
        testFixturesImplementation(rootProject.libs.org.hamcrest.core)
        testFixturesImplementation(rootProject.libs.org.mockito.core)
        testFixturesImplementation(rootProject.libs.org.awaitility.awaitility)
    }

    // Integration Tests
    tasks.withType<Copy>().all {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    sourceSets {
        create("intTest") {
            java.srcDir("src/intTest/java")
            resources.srcDir("src/intTest/resources")
            compileClasspath += sourceSets.main.get().output
            runtimeClasspath += sourceSets.main.get().output
        }
    }
    val intTestImplementation by configurations.getting {
        extendsFrom(configurations.implementation.get())
    }
    val intTestRuntimeOnly by configurations.getting {
        extendsFrom(configurations.runtimeOnly.get())
    }
    dependencies {
        intTestImplementation(rootProject.libs.junit.jupiter.api)
        intTestImplementation(rootProject.libs.org.hamcrest.core)
        intTestImplementation(rootProject.libs.org.mockito.core)
        intTestImplementation(rootProject.libs.org.awaitility.awaitility)

        intTestRuntimeOnly(rootProject.libs.junit.jupiter.engine)
    }
    val integrationTest = task<Test>("integrationTest") {
        // https://docs.gradle.org/7.2/userguide/java_testing.html#sec:test_execution
        useJUnitPlatform()
        maxHeapSize = "1G"

        description = "Runs integration tests."
        group = "verification"

        testClassesDirs = sourceSets["intTest"].output.classesDirs
        classpath = sourceSets["intTest"].runtimeClasspath
        shouldRunAfter("test")
    }

    // JARs
    tasks.named<Jar>("jar") {
        manifest {
            attributes(
                "Name" to "sh/props/",
                "Implementation-Title" to "sh.props",
                "Implementation-Version" to archiveVersion
            )
        }
    }

    // Checkstyle
    apply(plugin = "checkstyle")
    checkstyle {
        // will use the version declared in the catalog
        toolVersion = rootProject.libs.versions.checkstyle.get()
    }

    // Spotless
    apply(plugin = "com.diffplug.spotless")
    spotless {
        format("misc") {
            target("*.gradle", "*.md", ".gitignore")

            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }

        java {
            importOrder()
            removeUnusedImports()
            googleJavaFormat("1.10.0")
            trimTrailingWhitespace()
            endWithNewline()
            licenseHeaderFile(rootProject.file("props.license.kt")).updateYearWithLatest(true)
        }
    }

    // ErrorProne
    apply(plugin = "net.ltgt.errorprone")
    dependencies {
        errorprone(rootProject.libs.errorprone)
        errorprone(rootProject.libs.nullaway)
    }
    tasks.withType<JavaCompile>().configureEach {
        shouldRunAfter("spotlessJava")
        shouldRunAfter("spotlessApply")

        options.errorprone {
            disableWarningsInGeneratedCode.set(true)

            option("NullAway:AnnotatedPackages", "sh.props")
            // The check defaults to a warning, bump it up to an error for the main sources
            error("NullAway")
        }

        // needed to avoid ErrorProne logging an illegal reflective access warning
        options.isFork = true
        options.forkOptions.jvmArgs!!.addAll(
            listOf(
                "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED"
            )
        )
    }

    // IntelliJ IDE
    apply(plugin = "idea")
    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true

            testSourceDirs.addAll(java.sourceSets["intTest"].java.srcDirs)
            testSourceDirs.addAll(java.sourceSets["intTest"].resources.srcDirs)
        }
    }

    // Publish to GitHub Packages
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/props-sh/props")
                credentials {
                    username =
                        project.findProperty("gpr.username") as String? ?: System.getenv("USERNAME")
                    password =
                        project.findProperty("gpr.password") as String? ?: System.getenv("TOKEN")
                }
            }
            maven {
                name = "MavenCentral"
                url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                credentials {
                    username =
                        project.findProperty("ossrh.username") as String?
                            ?: System.getenv("OSSRH_USERNAME")
                    password = project.findProperty("ossrh.password") as String?
                        ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
        // Custom artifact IDs for subprojects
        // https://stackoverflow.com/a/67779953/7169815
    }
}
