

plugins {
    kotlin("jvm") version "1.9.23"

}
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.temporal:temporal-sdk:1.15.1")
 /*   implementation("com.expediagroup:graphql-kotlin-spring-server:5.3.0")
    implementation("org.springframework.boot:spring-boot-starter-web:2.6.3")*/
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")

    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("com.expediagroup:graphql-kotlin-server:7.0.0")
    implementation("com.expediagroup:graphql-kotlin-ktor-server:7.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")


    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}