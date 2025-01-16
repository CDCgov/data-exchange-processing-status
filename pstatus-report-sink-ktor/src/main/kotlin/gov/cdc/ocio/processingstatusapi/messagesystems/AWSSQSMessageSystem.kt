package gov.cdc.ocio.processingstatusapi.messagesystems

import aws.sdk.kotlin.services.sqs.SqsClient
import gov.cdc.ocio.processingstatusapi.health.messagesystem.HealthCheckAWSSQS
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Implementation of the AWS SQS message system.
 *
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class AWSSQSMessageSystem(sqsClient: SqsClient, queueUrl: String): MessageSystem {

    override var healthCheckSystem = HealthCheckAWSSQS(sqsClient, queueUrl) as HealthCheckSystem
}