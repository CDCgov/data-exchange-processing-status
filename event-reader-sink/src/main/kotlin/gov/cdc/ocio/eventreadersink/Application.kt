package gov.cdc.ocio.eventreadersink

import gov.cdc.ocio.eventreadersink.model.CloudProviderType
import gov.cdc.ocio.eventreadersink.exceptions.BadStateException
import gov.cdc.ocio.eventreadersink.exceptions.ConfigurationException
import gov.cdc.ocio.eventreadersink.exceptions.MissingPropertyException
import gov.cdc.ocio.eventreadersink.model.AwsConfig
import gov.cdc.ocio.eventreadersink.model.AzureConfig
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin
import gov.cdc.ocio.eventreadersink.model.CloudConfig
import gov.cdc.ocio.eventreadersink.plugins.configureRouting
import gov.cdc.ocio.eventreadersink.sink.CamelProcessor
import gov.cdc.ocio.eventreadersink.sink.EventProcessor
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val logger = KotlinLogging.logger {}


/**
 * Load the environment configuration values
 * Instantiate a singleton CloudConfig instance
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    try {
        val provider = environment.config.property("cloud.provider").getString()

        //Load CloudConfiguration based on the Cloud Provider
        val cloudConfig = when (provider) {
            CloudProviderType.AWS.value -> loadAwsConfig(environment)
            CloudProviderType.AZURE.value -> loadAzureConfig(environment)
            else -> throw IllegalArgumentException("Unsupported cloud provider: $provider")
        }

        // Define Koin module for CamelProcessor
        val camelModule = module {
            single(createdAtStart = true) { CamelProcessor() } // Creates a singleton instance of CamelProcessor
            single { EventProcessor(cloudConfig) }
        }

        return modules(listOf(camelModule))

    } catch (e: Exception) {
        // Log the error (replace with your logging mechanism)
        logger.error("Error loading Koin modules: ${e.message}")
        return modules(emptyList())
    }
}


/**
 * Loads AWS configuration from the given environment.
 * @param environment The ApplicationEnvironment from which to load the configuration.
 * @return A CloudConfig instance containing the AWS configuration.
 * @throws ConfigurationException if any required configuration property is missing or invalid.
 */
@Throws(MissingPropertyException::class, ConfigurationException::class)
private fun loadAwsConfig(environment: ApplicationEnvironment): CloudConfig {

    val awsConfig: AwsConfig
    return try {
        awsConfig = AwsConfig(
            accessKeyId = environment.config.property("cloud.aws.credentials.access_key_id").getString(),
            secretAccessKey = environment.config.property("cloud.aws.credentials.secret_access_key").getString(),
            sqsQueueName = environment.config.property("cloud.aws.sqs.queue_name").getString(),
            sqsQueueURL = environment.config.property("cloud.aws.sqs.queue_url").getString(),
            sqsRegion = environment.config.property("cloud.aws.sqs.region").getString(),
            s3EndpointURL = environment.config.propertyOrNull("cloud.aws.s3.endpoint_url")?.getString(),
            s3BucketName = environment.config.property("cloud.aws.s3.bucket_name").getString(),
            s3Region = environment.config.property("cloud.aws.s3.region").getString()
        )

        CloudConfig(provider = CloudProviderType.AWS, awsConfig = awsConfig)
    }catch (e: MissingPropertyException) {
        logger.error("Error: Missing required AWS configuration property: ${e.message}")
        throw MissingPropertyException("Missing required AWS configuration property.")
    } catch (e: Exception) {
        logger.error("Error loading AWS configuration: ${e.message}")
        throw ConfigurationException("Failed to load AWS configuration.")
    }
}


/**
 * Loads Azure configuration from the given environment.
 * @param environment The ApplicationEnvironment from which to load the configuration.
 * @return A CloudConfig instance containing the Azure configuration.
 * @throws ConfigurationException if any required configuration property is missing or invalid.
 */
@Throws(MissingPropertyException::class, ConfigurationException::class)
private fun loadAzureConfig(environment: ApplicationEnvironment): CloudConfig {
    val azureConfig: AzureConfig
    return try {
         azureConfig = AzureConfig(
            namespace = environment.config.property("cloud.azure.service_bus.namespace").getString(),
            connectionString = environment.config.property("cloud.azure.service_bus.connection_string").getString(),
            sharedAccessKeyName = environment.config.property("cloud.azure.service_bus.shared_access_key_name").getString(),
            sharedAccessKey = environment.config.property("cloud.azure.service_bus.shared_access_key").getString(),
            topicName = environment.config.property("cloud.azure.service_bus.topic_name").getString(),
            subscriptionName = environment.config.property("cloud.azure.service_bus.subscription_name").getString(),
            containerName = environment.config.property("cloud.azure.blob_storage.container_name").getString(),
            storageAccountKey = environment.config.property("cloud.azure.blob_storage.storage_account_key").getString(),
            storageAccountName = environment.config.property("cloud.azure.blob_storage.storage_account_name").getString()
        )

        CloudConfig(provider = CloudProviderType.AZURE, azureConfig = azureConfig)
    } catch (e: MissingPropertyException) {
        logger.error("Error: Missing required Azure configuration property: ${e.message}")
        throw ConfigurationException("Missing required Azure configuration property.")
    } catch (e: Exception) {
        logger.error("Error loading Azure configuration: ${e.message}")
        throw ConfigurationException("Failed to load Azure configuration.")
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

    configureRouting()

    install(ContentNegotiation) {
        jackson()
    }

    // Call the method from EventProcessor as soon as the application starts with the initial setup
    val eventProcessor: EventProcessor by inject()
    try {
        eventProcessor.processEvent()
    } catch (e: BadStateException) {
        logger.error("Application failed to start due to bad state: ${e.message}", e)
    } catch (e: Exception) {
        logger.error("Application failed to start due to an unexpected error: ${e.message}", e)
    }

}

