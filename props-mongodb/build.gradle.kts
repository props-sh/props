plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":props-core"))

    implementation("org.mongodb:mongodb-driver-sync:4.4.0")
    testFixturesImplementation("org.mongodb:mongodb-driver-sync:4.4.0")
    intTestImplementation(testFixtures(project(":props-mongodb")))
}
