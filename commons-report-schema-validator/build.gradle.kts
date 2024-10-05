plugins {
    kotlin("jvm") version "1.9.23"
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
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}