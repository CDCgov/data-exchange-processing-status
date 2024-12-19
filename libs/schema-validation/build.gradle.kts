


plugins {
    id ("org.jetbrains.kotlin.jvm") version "1.9.23"
    id ("java-library")
//    kotlin("jvm") version "1.9.23"
    // id ("org.jetbrains.kotlin.jvm") version "1.9.24"
    id ("io.ktor.plugin") version "2.3.11"

}

group = "gov.cdc.ocio"
version = "0.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass.set("gov.cdc.ocio.reportschemavalidator.service") // Replace with your main class
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

    implementation(project(":libs:commons-database"))
    implementation(project(":libs:commons-types"))
    implementation("io.insert-koin:koin-ktor:3.5.6")
    // AWS SDK for S3
    implementation("software.amazon.awssdk:s3:2.20.91")
    implementation("software.amazon.awssdk:auth:2.20.91")
    implementation("software.amazon.awssdk:regions:2.20.91")

    // Azure Blob Storage SDK
    implementation("com.azure:azure-storage-blob:12.25.0")
    implementation("com.azure:azure-identity:1.11.0")


    testImplementation(kotlin("test"))
   // testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation ("org.testng:testng:7.7.0")
    testImplementation ("org.mockito:mockito-inline:3.11.2")
    testImplementation ("io.mockk:mockk:1.13.9")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")


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
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
}



