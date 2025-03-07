package gov.cdc.ocio.processingnotifications.model


/**
 * DeadlineCheckSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by
 * the subscriptionId.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param daysToRun List<String>
 * @param timeToRun String
 * @param deliveryReference String
 */
data class DeadlineCheckSubscription(
    override val dataStreamId: String,
    override val dataStreamRoute: String,
    override val jurisdiction: String,
    override val daysToRun: List<String>,
    override val timeToRun: String,
    override val deliveryReference: String)
    : BaseSubscription(dataStreamId, dataStreamRoute ,jurisdiction, daysToRun, timeToRun, deliveryReference )

/**
 * DeadlineCheckUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow
 * by the subscriptionId.
 *
 * @param subscriptionId String
 */
data class DeadlineCheckUnSubscription(val subscriptionId:String)