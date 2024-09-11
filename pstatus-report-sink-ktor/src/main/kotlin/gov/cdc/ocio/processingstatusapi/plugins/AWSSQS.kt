package gov.cdc.ocio.processingstatusapi.plugins

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.DeleteMessageRequest
import aws.sdk.kotlin.services.sqs.model.QueueDoesNotExist

import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.SqsException

import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.qpid.proton.TimeoutException

/**
 * The `AWSSQServiceConfiguration` class configures and initializes connection AWS SQS based on settings provided in an `ApplicationConfig`.
 * This class extracts necessary AWS credentials and configuration details, such as the SQS queue URL, access key, secret key, and region,
 * using the provided configuration path as a prefix.
 * @param config `ApplicationConfig` containing the configuration settings for AWS SQS.
 * @param configurationPath represents prefix used to locate environment variables specific to AWS within the configuration.
 */
class AWSSQServiceConfiguration(config: ApplicationConfig, configurationPath: String? = null) {
    private val configPath = if (configurationPath != null) "$configurationPath." else ""
    val queueURL: String = config.tryGetString("${configPath}sqs.url") ?: ""
    private val accessKeyID = config.tryGetString("${configPath}access_key_id") ?: ""
    private val secretAccessKey = config.tryGetString("${configPath}secret_access_key") ?: ""
    private val region = config.tryGetString("${configPath}region") ?: "us-east-1"

    fun createSQSClient(): SqsClient{

        return SqsClient{ credentialsProvider = StaticCredentialsProvider {
            accessKeyId = this@AWSSQServiceConfiguration.accessKeyID
            secretAccessKey = this@AWSSQServiceConfiguration.secretAccessKey
        }; region = this@AWSSQServiceConfiguration.region }
    }
}

val AWSSQSPlugin = createApplicationPlugin(
    name = "AWS SQS",
    configurationPath = "aws",
    createConfiguration = ::AWSSQServiceConfiguration
) {
    lateinit var sqsClient: SqsClient
    lateinit var queueUrl: String

    try {
        sqsClient = pluginConfig.createSQSClient()
        queueUrl = pluginConfig.queueURL
        SchemaValidation.logger.info("Connection to the AWS SQS was successfully established")
    } catch (e: SqsException) {
        SchemaValidation.logger.error("Failed to create AWS SQS client ${e.message}")
    } catch (e: QueueDoesNotExist) {
        SchemaValidation.logger.error("AWS SQS URL provided does not exist ${e.message}")
    } catch (e: TimeoutException) {
        SchemaValidation.logger.error("Timeout occurred ${e.message}")
    } catch (e: Exception) {
        SchemaValidation.logger.error("Unexpected error occurred ${e.message}")
    }
    /**
     * The `consumeMessages` function continuously listens for and processes messages from an AWS SQS queue.
     * This function runs in a blocking coroutine, retrieving messages from the queue, validating them using
     * `AWSSQSProcessor`, and then deleting the processed messages from the queue.
     *
     * @throws Exception
     * @throws AwsServiceException
     */
    fun consumeMessages() {
        SchemaValidation.logger.info("Consuming messages from AWS SQS")
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val receiveMessageRequest = ReceiveMessageRequest {
                        this.queueUrl = queueUrl
                        maxNumberOfMessages = 5
                    }
                    val response = sqsClient.receiveMessage(receiveMessageRequest)
                    response.messages?.forEach { message ->
                        SchemaValidation.logger.info("Received message from AWS SQS: ${message.body}")

                        message.body?.let { AWSSQSProcessor().validateMessage(it) }
                        val deleteMessageRequest = DeleteMessageRequest {
                            this.queueUrl = queueUrl
                            this.receiptHandle = message.receiptHandle
                        }
                        sqsClient.deleteMessage(deleteMessageRequest)
                        SchemaValidation.logger.info("Deleted message from AWS SQS: ${message.body}")

                    }
                } catch (e: AwsServiceException) {
                    SchemaValidation.logger.error("Something went wrong while processing the request ${e.message}")
                } catch (e: Exception) {
                    SchemaValidation.logger.error("AWS service exception occurred: ${e.message}")
                }
            }
        }


    }
    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Application started successfully.")
        consumeMessages()
    }

    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Application stopped successfully.")
        cleanupResourcesAndUnsubscribe(application, sqsClient)
    }
}

/**
 * We need to clean up the resources and unsubscribe from application life events.
 *
 * @param application The Ktor instance, provides access to the environment monitor used
 * for unsubscribing from events.
 * @param sqsClient  `sqsClient` used to receive and then delete messages from AWS SQS
 */
private fun cleanupResourcesAndUnsubscribe(application: Application, sqsClient: SqsClient) {
    application.log.info("Closing SQS client")
    sqsClient.close()
    application.environment.monitor.unsubscribe(ApplicationStarted) {}
    application.environment.monitor.unsubscribe(ApplicationStopped) {}
}

/**
 * The main application module which runs always
 */
fun Application.awsSQSModule() {
    install(AWSSQSPlugin)
}
