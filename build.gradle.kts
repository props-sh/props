group = project.group
version = project.version

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        "classpath"(group = "com.google.googlejavaformat", name = "google-java-format", version = "1.10.0")
    }
    allprojects {
        repositories {
            mavenLocal()
        }
    }
}