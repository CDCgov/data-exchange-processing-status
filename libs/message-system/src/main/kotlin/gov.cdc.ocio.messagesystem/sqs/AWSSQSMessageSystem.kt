package gov.cdc.ocio.messagesystem.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.health.HealthCheckAWSSQS
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Implementation of the AWS SQS message system.
 *
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class AWSSQSMessageSystem(val sqsClient: SqsClient, queueUrl: String): MessageSystem {

    override var healthCheckSystem = HealthCheckAWSSQS(system, sqsClient, queueUrl) as HealthCheckSystem

    override fun send(message: String) {
        TODO("Not yet implemented")
    }
}