plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":props-core"))

    implementation("org.mongodb:mongodb-driver-sync:4.4.0")
    testFixturesImplementation("org.mongodb:mongodb-driver-sync:4.4.0")
    intTestImplementation(testFixtures(project(":props-mongodb")))

    intTestImplementation(platform("org.testcontainers:testcontainers-bom:1.16.2"))
    intTestImplementation("org.testcontainers:mongodb")
    intTestImplementation("org.testcontainers:junit-jupiter")
    intTestImplementation("ch.qos.logback:logback-core:1.2.7")
}
