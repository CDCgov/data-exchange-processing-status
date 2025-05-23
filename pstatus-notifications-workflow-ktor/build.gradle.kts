val ktorVersion: String by project
val kotlinxHtmlVersion: String by project

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "2.1.10"
    id("com.google.cloud.tools.jib") version "3.3.0"
    id ("io.ktor.plugin") version "2.3.11"
    id ("maven-publish")
    id ("java-library")
    id ("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
    id ("com.gorylenko.gradle-git-properties") version "2.4.2"
}

repositories {
    mavenCentral()
}

tasks.register("prepareKotlinBuildScriptModel"){}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("gov.cdc.ocio.processingnotifications.ApplicationKt")
}

dependencies {
    implementation(project(":libs:commons-database"))
    implementation(project(":libs:notification-dispatchers"))
    implementation(project(":libs:commons-types"))
    implementation("io.temporal:temporal-sdk:1.15.1")
    implementation("com.sendgrid:sendgrid-java:4.9.2")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0") // Java time module
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.2")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("joda-time:joda-time:2.12.7")
    implementation("org.apache.commons:commons-lang3:3.3.1")
    implementation("com.expediagroup:graphql-kotlin-server:6.0.0")
    implementation("com.expediagroup:graphql-kotlin-schema-generator:6.0.0")
    implementation("io.netty:netty-all:4.1.68.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.52.Final:windows-x86_64")
    implementation("software.amazon.awssdk:sts:2.29.34")
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.knowm.xchart:xchart:3.8.8")
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-2.0:2.15.0-alpha")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.49.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.49.0")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.30.1-alpha")

    testImplementation("io.insert-koin:koin-test:3.5.6")
    testImplementation("io.insert-koin:koin-test-junit5:4.0.4")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("IntegrationTest")
    }
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("IntegrationTest")
    }
}

kotlin {
    jvmToolchain(17)
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
        image = System.getenv("IMAGE") ?: ""
        tags = mutableSetOf(System.getenv("IMAGE_TAG") ?: "latest")
        auth {
            username = System.getenv("USERNAME") ?: ""
            password = System.getenv("PASSWORD") ?: ""
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
}

repositories{
    mavenCentral()
}

gitProperties {
    keys = listOf("git.commit.id", "git.commit.id.abbrev", "git.branch", "git.build.time", "git.commit.time")
    dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    dateFormatTimeZone = "UTC"
    extProperty = "gitProps"
}

// make sure the generateGitProperties task always executes (even when git.properties is not changed)
tasks.named("generateGitProperties").configure {
    outputs.upToDateWhen { false } // Forces task to run every time
}