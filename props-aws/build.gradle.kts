plugins {
    `java-library`
}

dependencies {
    implementation(project(":props-core"))
    implementation(platform(libs.aws))
    implementation("software.amazon.awssdk:secretsmanager")
}