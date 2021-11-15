plugins {
    `java-library`
}

dependencies {
    implementation(project(":props-core"))

    implementation(platform("software.amazon.awssdk:bom:2.17.80"))
    implementation("software.amazon.awssdk:secretsmanager")
}

//sourceSets {
//    create("intTest") {
//        compileClasspath += sourceSets.main.get().output
//        runtimeClasspath += sourceSets.main.get().output
//    }
//}
//
//
//val intTestImplementation by configurations.getting {
//    extendsFrom(configurations.implementation.get())
//}
//
//configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
//
//dependencies {
//    intTestImplementation(rootProject.libs.junit.jupiter.api)
//    intTestImplementation(rootProject.libs.org.hamcrest.core)
//    intTestImplementation(rootProject.libs.org.mockito.core)
//    intTestImplementation(rootProject.libs.org.awaitility.awaitility)
//}
//
//val integrationTest = task<Test>("integrationTest") {
//    description = "Runs integration tests."
//    group = "verification"
//
//    testClassesDirs = sourceSets["intTest"].output.classesDirs
//    classpath = sourceSets["intTest"].runtimeClasspath
//    shouldRunAfter("test")
//}
//
//tasks.check { dependsOn(integrationTest) }