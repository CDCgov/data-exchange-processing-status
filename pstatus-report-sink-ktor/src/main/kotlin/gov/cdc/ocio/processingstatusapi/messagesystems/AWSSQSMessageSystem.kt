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
class AWSSQSMessageSystem(val sqsClient: SqsClient, queueUrl: String): MessageSystem {

    override var healthCheckSystem = HealthCheckAWSSQS(system, sqsClient, queueUrl) as HealthCheckSystem
}