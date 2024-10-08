package gov.cdc.ocio.processingstatusapi.health.messagesystem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.rabbitmq.client.Connection
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.health.HealthCheck
import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem
import gov.cdc.ocio.processingstatusapi.plugins.RabbitMQServiceConfiguration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the RabbitMQ messaging service health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckRabbitMQ : HealthCheckSystem("RabbitMQ"), KoinComponent {

    private val rabbitMQServiceConfiguration by inject<RabbitMQServiceConfiguration>()

    /**
     * Checks and sets rabbitMQHealth status
     */
    override fun doHealthCheck() {
        try {
            if (isRabbitMQHealthy()) {
                status = HealthCheck.STATUS_UP
            }
        } catch (ex: Exception){
            logger.error("RabbitMQ is not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether rabbitMQ is healthy.
     *
     * @return Boolean
     */
    @Throws(BadStateException::class)
    private fun isRabbitMQHealthy(): Boolean {
        var rabbitMQConnection: Connection? = null
        return try {
            rabbitMQConnection = rabbitMQServiceConfiguration.getConnectionFactory().newConnection()
            rabbitMQConnection.isOpen
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to RabbitMQ server.")
        } finally {
            rabbitMQConnection?.close()
        }
    }
}