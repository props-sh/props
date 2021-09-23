import net.ltgt.gradle.errorprone.errorprone

repositories {
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    `java-library`
    checkstyle
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    id("me.champeau.jmh")
}

dependencies {
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.org.hamcrest.core)
    testImplementation(libs.org.mockito.core)

    testRuntimeOnly(libs.junit.jupiter.engine)
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
        removeUnusedImports()
        googleJavaFormat("1.9")
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(rootProject.file("props.license.kt"))
    }
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()

    // https://docs.gradle.org/7.2/userguide/java_testing.html#sec:test_execution
    maxHeapSize = "1G"
}

// needed to avoid ErrorProne logging an illegal reflective access warning
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.jvmArgs!!.addAll(listOf(
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
    ))
}
tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        option("NullAway:AnnotatedPackages", "sh.props")
    }
}
tasks.named("compileJava", JavaCompile::class) {
    // The check defaults to a warning, bump it up to an error for the main sources
    options.errorprone.error("NullAway")
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

jmh {
    iterations.set(1)
}
