package gov.cdc.ocio.messagesystem.plugins

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.ClientException

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.*
import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import gov.cdc.ocio.messagesystem.sqs.AWSSQSMessageSystem
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.config.AWSSQSServiceConfiguration

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.qpid.proton.TimeoutException
import org.koin.java.KoinJavaComponent.getKoin


val AWSSQSPlugin = createApplicationPlugin(
    name = "AWS SQS",
    configurationPath = "aws",
    createConfiguration = ::AWSSQSServiceConfiguration
) {

    val logger = KotlinLogging.logger {}

    lateinit var sqsClient: SqsClient

    lateinit var queueUrl: String

    try {
        queueUrl = pluginConfig.listenQueueURL
        logger.info("Connection to the AWS SQS was successfully established")
    } catch (e: SqsException) {
        logger.error("Failed to create AWS SQS client ${e.message}")
    } catch (e: QueueDoesNotExist) {
        logger.error("AWS SQS URL provided does not exist ${e.message}")
    } catch (e: TimeoutException) {
        logger.error("Timeout occurred ${e.message}")
    } catch (e: Exception) {
        logger.error("Unexpected error occurred ${e.message}")
    }

    /**
     * Deletes messages from AWS SQS Service that has been validated
     * @param receivedMessages the list of message(s) received from the queue to be deleted
     * @throws Exception
     */
    suspend fun deleteMessage(receivedMessages: ReceiveMessageResponse) {
        receivedMessages.messages?.forEach { message ->
            try {
                retryWithBackoff(numOfRetries = 5) {
                    val deleteMessageRequest = DeleteMessageRequest {
                        this.queueUrl = queueUrl
                        this.receiptHandle = message.receiptHandle
                    }
                    sqsClient.deleteMessage(deleteMessageRequest)
                }
                logger.info("Successfully deleted processed report from AWS SQS")
            } catch (e: Exception) {
                logger.error("Something went wrong while deleting the report from the queue ${e.message}")
            }
        }
    }

    /**
     * Validates messages from the AWS SQS Service.
     *
     * @param receivedMessages the list of message(s) received from the queue to be validated
     * @throws Exception thrown during validation `and` it's important to delete the message as it will be persisted to
     * dead-letter container in configured database.
     */
    suspend fun validate(receivedMessages: ReceiveMessageResponse) {
        try {
            receivedMessages.messages?.forEach { message ->
                logger.info("Received message from AWS SQS")
                val awsSQSProcessor = pluginConfig.messageProcessor
                message.body?.let {
                   awsSQSProcessor.processMessage(it)
                }
            }
            deleteMessage(receivedMessages)
        } catch (e: Exception) {
            logger.error("An Exception occurred during validation ${e.message}")
            deleteMessage(receivedMessages)
        }
    }

    /**
     * The `consumeMessages` function continuously listens for and processes messages from an AWS SQS queue.
     * This function runs in a non-blocking coroutine, retrieving messages from the queue, validating them using
     * `AWSSQSProcessor`, and then deleting the processed messages from the queue.
     *
     * @throws Exception
     * @throws AwsServiceException
     */
    fun consumeMessages() {
        logger.info("Consuming messages from AWS SQS")
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                var receivedMessages: ReceiveMessageResponse?
                try {
                    receivedMessages = retryWithBackoff(numOfRetries = 5){
                        val receiveMessageRequest = ReceiveMessageRequest {
                            this.queueUrl = queueUrl
                            maxNumberOfMessages = 5
                        }
                        sqsClient.receiveMessage(receiveMessageRequest)
                    }
                   validate(receivedMessages)
                } catch (e: AwsServiceException) {
                    logger.error("AwsServiceException occurred while processing the request ${e.message} with requestID: ${e.sdkErrorMetadata.requestId}")
                    throw e
                } catch (e: ClientException) {
                    logger.error("ClientException occurred either while trying to send request to AWS or while trying to parse a response from AWS ${e.message}")
                    throw e
                } catch (e: Exception) {
                    logger.error("AWS service exception occurred: ${e.message}")
                    throw e
                }
            }
        }
    }

    on(MonitoringEvent(ApplicationStarted)) {
        logger.info("Application started successfully.")
        val msgSystem = getKoin().get<MessageSystem>() as AWSSQSMessageSystem
        sqsClient = msgSystem.sqsClient
        consumeMessages()
    }

    on(MonitoringEvent(ApplicationStopped)) {
        logger.info("Application stopped successfully.")
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
private fun cleanupResourcesAndUnsubscribe(
    application: Application,
    sqsClient: SqsClient
) {
    val logger = KotlinLogging.logger {}

    logger.info("Closing SQS client")
    sqsClient.close()
    application.environment.monitor.unsubscribe(ApplicationStarted) {}
    application.environment.monitor.unsubscribe(ApplicationStopped) {}
}

/**
 * The main application module which runs always
 */
fun Application.awsSQSModule(messageProcessorImpl: MessageProcessorInterface) {
    install(AWSSQSPlugin) {
        messageProcessor = messageProcessorImpl
    }
}

/**
 * The `retryWithBackoff` retries block of code with exponential backoff, doubling the delay before each retry
 * until `maxDelay` is reached or specified number of retries is exhausted.
 *
 * @param numOfRetries The number of times to retry attempts. Default is 3.
 * @param baseDelay The initial delay between retries in milliseconds. Default is 1000 ms.
 * @param maxDelay The maximum delay between retries, in milliseconds. Default is 6000 ms.
 * @param block The block of code to be executed.
 *
 */
suspend fun<P> retryWithBackoff(
    numOfRetries: Int = 3,
    baseDelay:Long = 1000,
    maxDelay: Long = 6000,
    block:suspend()-> P
): P {
    val logger = KotlinLogging.logger {}

    var currentDelay = baseDelay
    repeat(numOfRetries) {
        try {
            return block()
        } catch (e:Exception) {
            logger.error("Attempt failed with exception: ${e.message}. Retrying again in $currentDelay")
            delay(currentDelay)
            currentDelay = (currentDelay *2).coerceAtMost(maxDelay)
        }
    }
    // This is the last attempt, and if it fails again will throw an exception
    logger.error("Last Attempt, if it fails again exception will be thrown")
    return block()
}
