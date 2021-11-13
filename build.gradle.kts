import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    idea
    checkstyle
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
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
            version = "1.10.0"
        )
    }
    allprojects {
        repositories {
            // uncomment if you need to use the local Maven cache
            // mavenLocal()
        }
    }
}

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    apply(plugin = "idea")
    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

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
    tasks.getByName<Test>("test") {
        useJUnitPlatform()

        // https://docs.gradle.org/7.2/userguide/java_testing.html#sec:test_execution
        maxHeapSize = "1G"
    }

    // tasks.jar
    tasks.named<Jar>("jar") {
        archiveFileName.set("foo.jar")
        manifest {
            attributes(
                "Name" to "sh/props/",
                "Implementation-Title" to "sh.props",
                "Implementation-Version" to archiveVersion
            )
        }
    }

    tasks.create<Zip>("docZip") {
        archiveFileName.set("doc.zip")
        from("doc")
    }

    apply(plugin = "checkstyle")
    checkstyle {
        // will use the version declared in the catalog
        toolVersion = rootProject.libs.versions.checkstyle.get()
    }

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
            licenseHeaderFile(rootProject.file("props.license.kt"))
        }
    }

    apply(plugin = "net.ltgt.errorprone")
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

    dependencies {
        errorprone(rootProject.libs.errorprone)
        errorprone(rootProject.libs.nullaway)

        testImplementation(rootProject.libs.junit.jupiter.api)
        testImplementation(rootProject.libs.org.hamcrest.core)
        testImplementation(rootProject.libs.org.mockito.core)
        testImplementation(rootProject.libs.org.awaitility.awaitility)

        testRuntimeOnly(rootProject.libs.junit.jupiter.engine)
    }
}
