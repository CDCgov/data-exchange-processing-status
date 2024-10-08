package gov.cdc.ocio.processingstatusapi.health.messagesystem

import aws.sdk.kotlin.services.sqs.SqsClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.health.HealthCheck
import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem
import gov.cdc.ocio.processingstatusapi.plugins.AWSSQServiceConfiguration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the AWS SQS messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckAWSSQS : HealthCheckSystem("AWS SQS"), KoinComponent {

    private val awsSqsServiceConfiguration by inject<AWSSQServiceConfiguration>()

    /**
     * Checks and sets AWSSQSHealth status
     *
     * @return HealthCheckAWSSQS object with updated status
     */
    override fun doHealthCheck() {
        try {
            if (isAWSSQSHealthy(awsSqsServiceConfiguration)) {
                status = HealthCheck.STATUS_UP
            }

        } catch (ex: Exception) {
            logger.error("AWS SQS is not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether AWS SQS is healthy.
     *
     * @return Boolean
     */
    @Throws(BadStateException::class)
    private fun isAWSSQSHealthy(config: AWSSQServiceConfiguration): Boolean {
        val sqsClient: SqsClient?
        return try {
            sqsClient = config.createSQSClient()
            sqsClient.close()
            true
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to AWS SQS service.")
        }
    }
}