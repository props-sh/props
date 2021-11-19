plugins {
    `java-library`
}

dependencies {
    implementation(project(":props-core"))

    implementation("org.mongodb:mongodb-driver-reactivestreams:4.4.0")
}
