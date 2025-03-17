package gov.cdc.ocio.messagesystem.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.health.HealthCheckAWSSQS
import gov.cdc.ocio.types.health.HealthCheckSystem
import kotlinx.coroutines.runBlocking


/**
 * Implementation of the AWS SQS message system.
 *
 * @property sqsClient SqsClient
 * @property sendQueueUrl String
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class AWSSQSMessageSystem(
    val sqsClient: SqsClient,
    listenQueueUrl: String,
    private val sendQueueUrl: String
): MessageSystem {

    override var healthCheckSystem = HealthCheckAWSSQS(system, sqsClient, listenQueueUrl) as HealthCheckSystem

    /**
     * Sends a message to a queue.
     *
     * @param message String
     */
    override fun send(message: String) {
        runBlocking {
            val request = SendMessageRequest {
                queueUrl = sendQueueUrl
                messageBody = message
            }
            sqsClient.sendMessage(request)
        }
    }
}