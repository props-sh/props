group = "sh.props"
version = project.version

subprojects {
    buildscript {
        repositories {
            mavenCentral()
        }
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
