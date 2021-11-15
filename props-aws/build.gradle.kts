plugins {
    `java-library`
}

dependencies {
    implementation(project(":props-core"))

    implementation(platform("software.amazon.awssdk:bom:2.17.80"))
    implementation("software.amazon.awssdk:secretsmanager")
}
