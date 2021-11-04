group = project.group
version = project.version

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        "classpath"(group = "com.google.googlejavaformat", name = "google-java-format", version = "1.10.0")
    }
}