

buildscript {
    repositories {
        mavenCentral()
    }

}
plugins {
    kotlin("jvm") version "1.9.23"
    id("com.google.cloud.tools.jib") version "3.3.0"
    id ("io.ktor.plugin") version "2.3.11"
    id ("maven-publish")
    id ("java-library")
    id ("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
}
repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group "gov.cdc.ocio"
version "0.0.1"

dependencies {
    implementation("io.temporal:temporal-sdk:1.15.1")
    implementation("com.sendgrid:sendgrid-java:4.9.2")
    implementation ("io.ktor:ktor-server-core:2.3.2")
    implementation ("io.ktor:ktor-server-netty:2.3.2")
    implementation ("io.ktor:ktor-server-content-negotiation:2.3.2")
    implementation ("io.ktor:ktor-serialization-kotlinx-json:2.3.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.2")
    implementation ("io.github.microutils:kotlin-logging-jvm:3.0.5")
   implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation ("org.slf4j:slf4j-api:1.7.36")
    implementation ("ch.qos.logback:logback-classic:1.4.12")
    implementation ("io.insert-koin:koin-core:3.5.6")
    implementation ("io.insert-koin:koin-ktor:3.5.6")
    implementation ("com.sun.mail:javax.mail:1.6.2")
    implementation ("com.expediagroup:graphql-kotlin-ktor-server:7.1.1")
    implementation ("com.graphql-java:graphql-java-extended-scalars:22.0")
    implementation ("joda-time:joda-time:2.12.7")
    implementation ("org.apache.commons:commons-lang3:3.3.1")
    implementation ("com.expediagroup:graphql-kotlin-server:6.0.0")
    implementation ("com.expediagroup:graphql-kotlin-schema-generator:6.0.0")
    implementation ("io.ktor:ktor-server-netty:2.1.0")
    implementation ("io.ktor:ktor-client-content-negotiation:2.1.0")
    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}

repositories{
    mavenLocal()
    mavenCentral()
}

ktor {
    docker {
        localImageName.set("pstatus-notifications-workflow-ktor")
    }
}

jib {
    from {
        auth {
            username = System.getenv("DOCKERHUB_USERNAME") ?: ""
            password = System.getenv("DOCKERHUB_TOKEN") ?: ""
        }
    }
    to {
        image = "imagehub.cdc.gov:6989/dex/pstatus/notifications-workflow-service"
        auth {
            username = System.getenv("IMAGEHUB_USERNAME") ?: ""
            password = System.getenv("IMAGEHUB_PASSWORD") ?: ""
        }
    }
}

repositories{
    mavenCentral()
}

