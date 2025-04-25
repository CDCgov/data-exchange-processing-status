package gov.cdc.ocio.messagesystem.health

import kotlinx.coroutines.*
import aws.sdk.kotlin.services.sqs.SqsClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the AWS SQS messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckAWSSQS(
    system: String,
    private val sqsClient: SqsClient?,
    private val queueUrl: String,
) : HealthCheckSystem(system, "AWS SQS") {

    /**
     * Checks the AWS SQS health.
     *
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = runBlocking {
            async {
                isAWSSQSHealthy()
            }.await()
        }
        result.onFailure { error ->
            val reason = "AWS SQS is not healthy: ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether AWS SQS is healthy.
     * @return Result<Boolean>
     */
    private suspend fun isAWSSQSHealthy(): Result<Boolean> {
        if (sqsClient == null) return Result.failure(Exception("Unable to obtain an SQS client"))
        return try {
            val response = sqsClient.listQueues()
            if (response.queueUrls?.contains(queueUrl) == true)
                Result.success(true)
            else
                Result.failure(Exception("The expected queue URL is missing"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to establish connection to AWS SQS service: ${e.localizedMessage}"))
        }
    }
}