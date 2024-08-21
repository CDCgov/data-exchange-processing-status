

plugins {
    kotlin("jvm") version "1.9.23"

}
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.temporal:temporal-sdk:1.15.1")
     testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}