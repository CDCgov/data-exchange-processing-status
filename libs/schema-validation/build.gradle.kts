

plugins {
    kotlin("jvm") version "1.9.23"
   // id ("org.jetbrains.kotlin.jvm") version "1.9.24"
    id ("io.ktor.plugin") version "2.3.11"
}

group = "gov.cdc.ocio"
version = "0.0.1"

repositories {
    mavenCentral()
}


dependencies {
    // JSON validations
    implementation ("com.sun.activation:javax.activation:1.2.0")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.networknt:json-schema-validator:1.0.73")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
    implementation("io.github.microutils","kotlin-logging-jvm", "3.0.5")
    implementation ("org.slf4j", "slf4j-api", "1.7.36")
    implementation ("ch.qos.logback", "logback-classic", "1.5.7")
    implementation ("ch.qos.logback.contrib", "logback-json-classic", "0.1.5")
    implementation ("ch.qos.logback.contrib", "logback-jackson", "0.1.5")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.sun.activation:javax.activation:1.2.0")
    testImplementation(kotlin("test"))
   // testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation ("org.testng:testng:7.7.0")
    testImplementation ("org.mockito:mockito-inline:3.11.2")
    testImplementation ("io.mockk:mockk:1.13.9")
    testImplementation("io.ktor:ktor-server-tests-jvm")

}

tasks.test {
    useTestNG()
    testLogging {
        events ("passed", "skipped", "failed")
    }
    //Change this to "true" if we want to execute unit tests
    systemProperty("isTestEnvironment", "false")

    // Set the test classpath, if required
}
kotlin {
    jvmToolchain(17)
}

