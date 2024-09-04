package gov.cdc.ocio.pstatus.topic.sink.blob

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin
import gov.cdc.ocio.pstatus.topic.sink.blob.camel.*
import gov.cdc.ocio.pstatus.topic.sink.model.CloudConfig
import org.koin.dsl.module

/**
 * Load the environment configuration values
 * Instantiate a singleton CloudConfig instance
 * @param environment ApplicationEnvironment
 */

fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment) {

    val provider = environment.config.property("cloud.provider").getString()
    val cloudConfig: CloudConfig

    when (provider) {
        "aws" -> {
            val awsAccessKeyId = environment.config.property("cloud.aws.credentials.access_key_id").getString()
            val awsSecretAccessKey = environment.config.property("cloud.aws.credentials.secret_access_key").getString()
            val awsSqsQueueName = environment.config.property("cloud.aws.sqs.queue_name").getString()
            val awsSqsQueueURL = environment.config.property("cloud.aws.sqs.queue_url").getString()
            val awsSqsRegion = environment.config.property("cloud.aws.sqs.region").getString()
            val awsS3BucketName = environment.config.property("cloud.aws.s3.bucket_name").getString()
            val awsS3Region = environment.config.property("cloud.aws.s3.region").getString()

            cloudConfig = CloudConfig(provider, awsAccessKeyId, awsSecretAccessKey, awsSqsQueueName, awsSqsQueueURL, awsSqsRegion, awsS3BucketName, awsS3Region,"", "", "", "", "", "")
            CamelProcessor().sinkMessageToStorage(cloudConfig)

        }
        "azure" -> {
            val namespace = environment.config.property("cloud.azure.service_bus.namespace").getString()
            val connectionString = environment.config.property("cloud.azure.service_bus.connection_string").getString()
            val sharedAccessKeyName = environment.config.property("cloud.azure.service_bus.shared_access_key_name").getString()
            val sharedAccessKey = environment.config.property("cloud.azure.service_bus.shared_access_key").getString()
            val topicName = environment.config.property("cloud.azure.service_bus.topic_name").getString()
            val subscriptionName = environment.config.property("cloud.azure.service_bus.subscription_name").getString()
            val containerName = environment.config.property("cloud.azure.blob_storage.container_name").getString()
            val storageAccountKey = environment.config.property("cloud.azure.blob_storage.storage_account_key").getString()
            val storageAccountName = environment.config.property("cloud.azure.blob_storage.storage_account_name").getString()

            cloudConfig = CloudConfig(provider, "", "", "", "", "", "", "", namespace, connectionString, sharedAccessKeyName, sharedAccessKey, topicName, subscriptionName, containerName, storageAccountKey, storageAccountName)
            CamelProcessor().sinkMessageToStorage(cloudConfig)
        }
        else -> throw IllegalArgumentException("Unsupported cloud provider")
    }
}



/**
 * The main function
 *  @param args Array<string>
 */

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}


/**
 * The main application module which always runs and loads other modules
 */

fun Application.module() {

    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}

