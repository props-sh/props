group = project.group
version = project.version

buildscript {
    repositories {
        // uncomment if you need to use the local Maven cache
        // mavenLocal()
        mavenCentral()
    }
    dependencies {
        "classpath"(group = "com.google.googlejavaformat", name = "google-java-format", version = "1.10.0")
    }
    allprojects {
        repositories {
        // uncomment if you need to use the local Maven cache
        // mavenLocal()
        }
    }
}
