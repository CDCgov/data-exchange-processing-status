package gov.cdc.ocio.functions.servicebus

import gov.cdc.ocio.exceptions.BadRequestException
import gov.cdc.ocio.exceptions.BadStateException
import gov.cdc.ocio.exceptions.InvalidSchemaDefException
import gov.cdc.ocio.model.message.SchemaDefinition
import gov.cdc.ocio.model.message.StatusType
import gov.cdc.ocio.model.message.SubscriptionType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.qpid.proton.codec.transport.DispositionType
import java.util.*

/**
 * The subscription manager interacts directly with CosmosDB or cache to subscribe and unsubscribe emails and websockets.
 *
 * @property context ExecutionContext
 * @constructor
 */
class SubscriptionManager {

    private val logger = KotlinLogging.logger {}

//    private val reportsContainerName = "Reports"
//    private val partitionKey = "/uploadId"

//    private val reportsContainer by lazy {
//        CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)
//    }

    /**
     * Create a subscription with the provided details.
     * @param destinationId String
     * @param eventType String
     * @param stageName String
     * @param email SubscriptionType
     * @param statusType StatusType
     * @param contentType String
     * @return UUID - subscription identifier of uuid
     * @throws BadStateException
     * @throws BadRequestException
     */
    @Throws(BadStateException::class, BadRequestException::class)
    fun subscribeForEmail(
        destinationId: String,
        eventType: String,
        email: SubscriptionType,
        stageName: String,
        statusType: StatusType,
        content: String,
        contentType: String,
    ): String {
        // Verify the content contains the minimum schema information
        try {
            SchemaDefinition.fromJsonString(content)
        } catch(e: InvalidSchemaDefException) {
            throw BadRequestException("Invalid schema definition: ${e.localizedMessage}")
        }

        return createSubscription(destinationId, eventType, stageName, email, statusType, contentType)
    }

    /**
     * Create the provided report.  Note the dispositionType indicates whether this will add or replace existing
     * report(s) with this stageName.
     * @param destinationId String
     * @param eventType String
     * @param stageName String
     * @param source SubscriptionType
     * @param statusType StatusType - Indicates weather to subscribe for success, warnings or failures
     * @param contentType String
     * given stageName.
     * @return String - subscription identifier of uuid
     * */
    private fun createSubscription(destinationId: String,
                             eventType: String,
                             stageName: String,
                             source: SubscriptionType,
                             statusType: StatusType,
                             contentType: String,): String {

        when (source) {
            SubscriptionType.SUBSCRIBE_EMAIL -> {
                logger.info("Subscribe fr the given email id for " +
                        "provided destination Id $destinationId, " +
                        "eventType $eventType and stageName $stageName")

            }
            SubscriptionType.SUBSCRIBE_WEBSOCKET -> {
                logger.info("Subscribe fr the given email id for " +
                        "provided destination Id $destinationId, " +
                        "eventType $eventType and stageName $stageName")

            }
            SubscriptionType.UNSUBSCRIBE -> {
                logger.info("Subscribe fr the given email id for " +
                        "provided destination Id $destinationId, " +
                        "eventType $eventType and stageName $stageName")

            }
            else -> {}
        }

        return UUID.randomUUID().toString()
    }

}