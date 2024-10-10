import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.Companion.test


buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
    }
}
plugins {
    id ("org.jetbrains.kotlin.jvm") version "1.9.24"
    id ("io.ktor.plugin") version "2.3.11"
    id("java") // This is required to work with the `test` task
}




dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation ("io.ktor:ktor-server-content-negotiation:1.9.24")
    implementation ("io.ktor:ktor-serialization-jackson:1.9.24")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("com.azure:azure-messaging-servicebus:7.13.3")
    implementation("com.azure:azure-cosmos:4.55.0")
    implementation("com.rabbitmq:amqp-client:5.21.0")
    implementation("aws.sdk.kotlin:sqs:1.0.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation ("com.microsoft.azure.functions:azure-functions-java-library:3.0.0")
    implementation ("com.sun.activation:javax.activation:1.2.0")
    implementation ("com.microsoft.azure:applicationinsights-core:3.4.19")
    implementation ("com.azure:azure-cosmos:4.55.0")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation ("io.opentelemetry:opentelemetry-api:1.29.0")
    // JSON validations
   /* testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation ("org.testng:testng:7.7.0")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
    testImplementation ("org.mockito:mockito-inline:3.11.2")
    testImplementation ("io.mockk:mockk:1.13.9")*/

    // Kotlin dependencies
    implementation(kotlin("stdlib"))

    // TestNG dependency
    testImplementation("org.testng:testng:7.7.0")

    // Mockk for mocking
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation ("org.mockito:mockito-inline:3.11.2")
    // Kotlin test dependencies for assertions
    testImplementation(kotlin("test"))

}
tasks.test {
    // Use TestNG as the test framework
    useTestNG()

    // Configure test logging
    testLogging {
        events("passed", "skipped", "failed")
    }

    // Add system property for conditional behavior in tests
    systemProperty("uri", "https://ocio-ede-dev-processingstatus-test-db.documents.azure.com:443/")
    systemProperty("authKey", "") // get it from portal
    systemProperty("containerName", "Rules")
    systemProperty("partitionKey", "/ruleId")
    // You can set other classpath or configurations if required
}
/*
jib {
    from {
        auth {
            username = System.getenv("DOCKERHUB_USERNAME") ?: ""
            password = System.getenv("DOCKERHUB_TOKEN") ?: ""
        }
    }
    to {
        image = 'imagehub.cdc.gov:6989/dex/pstatus/report-sink-service'
        tags = [ System.getenv("IMAGE_TAG") ?: "latest" ]
        auth {
            username = System.getenv("IMAGEHUB_USERNAME") ?: ""
            password = System.getenv("IMAGEHUB_PASSWORD") ?: ""
        }
    }
}*/

repositories {
    mavenCentral()
}
