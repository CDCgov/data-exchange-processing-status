

plugins {
    kotlin("jvm") version "1.9.23"

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    dependencies {
        implementation("org.jeasy:easy-rules-core:4.1.0")
        implementation("org.jeasy:easy-rules-mvel:4.1.0")
        implementation("org.jeasy:easy-rules-support:4.1.0")
        implementation("org.yaml:snakeyaml:1.29")
    }
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}