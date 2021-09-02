import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    checkstyle
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

dependencies {
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_11.toString()))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.compileJava {
    options.release.set(11)
    options.isIncremental = true
    options.isFork = true
    options.isFailOnError = true
}

checkstyle {
    // will use the version declared in the catalog
    toolVersion = libs.versions.checkstyle.get()
}

spotless {
    format("misc") {
        target("*.gradle", "*.md", ".gitignore")

        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }

    java {
        googleJavaFormat("1.9").aosp()
        licenseHeaderFile(rootProject.file("props.license.kt"))
    }
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()

    // https://docs.gradle.org/7.2/userguide/java_testing.html#sec:test_execution
    maxHeapSize = "1G"
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
}

tasks.jar {
    manifest {
        attributes(
                "Implementation-Title" to "Gradle",
                "Implementation-Version" to archiveVersion
        )
    }
}

tasks.create<Zip>("docZip") {
    archiveFileName.set("doc.zip")
    from("doc")
}
