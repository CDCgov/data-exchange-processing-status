val ktorVersion: String by project
val kotlinxHtmlVersion: String by project

plugins {
    id ("org.jetbrains.kotlin.jvm") version "2.1.10"
    id ("java-library")
    id ("io.ktor.plugin") version "2.3.11"
}

group = "gov.cdc.ocio"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:commons-types"))
    implementation("io.github.microutils","kotlin-logging-jvm", "3.0.5")
    implementation ("org.slf4j", "slf4j-api", "1.7.36")
    implementation ("ch.qos.logback", "logback-classic", "1.5.7")
    implementation ("ch.qos.logback.contrib", "logback-json-classic", "0.1.5")
    implementation ("ch.qos.logback.contrib", "logback-jackson", "0.1.5")
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.ktor:ktor-client-cio:$ktorVersion") // for invoking webhooks

    implementation ("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion") // for building HTML content
    implementation("com.sun.mail:jakarta.mail:2.0.1") // for sending emails

    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation ("org.mockito:mockito-inline:3.11.2")
    testImplementation ("io.mockk:mockk:1.13.9")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jsoup:jsoup:1.17.2") // for EmailBuilder unit tests
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
}
