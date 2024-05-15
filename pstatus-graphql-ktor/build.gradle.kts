
val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.10"
}

group = "gov.cdc.ocio.processingstatusapi"
version = "0.0.1"

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
        localImageName.set("pstatus-graphql-ktor-docker-image")
        imageTag.set("pstatus-graphql-ktor")

        environmentVariable("NAME", "\"Container\"")
    }
}

application {
    mainClass.set("gov.cdc.ocio.processingstatusapi.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.expediagroup", "graphql-kotlin-ktor-server", "7.1.1")
    implementation("com.azure", "azure-messaging-servicebus", "7.13.3")
    implementation("com.azure", "azure-cosmos", "4.55.0")
    implementation("io.github.microutils", "kotlin-logging-jvm", "3.0.5")
    implementation("com.google.code.gson", "gson", "2.10.1")
    implementation("io.insert-koin","koin-core","3.5.6")
    implementation("io.insert-koin", "koin-ktor", "3.5.6")
    implementation("com.graphql-java", "graphql-java-extended-scalars", "22.0")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
