


plugins {
    id ("org.jetbrains.kotlin.jvm") version "1.9.23"
    id ("java-library")
}

group = "gov.cdc.ocio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.register("prepareKotlinBuildScriptModel"){}

dependencies {
    implementation(kotlin("stdlib"))
  //  implementation ("io.ktor:ktor-serialization-jackson:1.9.24")
    implementation("io.insert-koin:koin-core:3.4.0") // Add this if missing
    implementation(project(":libs:commons-database"))
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.23")
    implementation ("com.sun.activation:javax.activation:1.2.0")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.networknt:json-schema-validator:1.0.73")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
    implementation("io.github.microutils","kotlin-logging-jvm", "3.0.5")
    implementation ("org.slf4j", "slf4j-api", "1.7.36")
    implementation ("ch.qos.logback", "logback-classic", "1.5.7")
    implementation ("ch.qos.logback.contrib", "logback-json-classic", "0.1.5")
    implementation ("ch.qos.logback.contrib", "logback-jackson", "0.1.5")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

