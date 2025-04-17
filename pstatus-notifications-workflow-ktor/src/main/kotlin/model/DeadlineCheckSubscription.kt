package gov.cdc.ocio.processingnotifications.model


/**
 * DeadlineCheckSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by
 * the subscriptionId.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param cronSchedule String
 * @param emailAddresses List<String>
 */
data class DeadlineCheckSubscription(
    override val dataStreamId: String,
    override val dataStreamRoute: String,
    override val jurisdiction: String,
    override val cronSchedule: String,
    override val emailAddresses: List<String>?,
    override val webhookUrl: String?
) : BaseSubscription(dataStreamId, dataStreamRoute, jurisdiction, cronSchedule, emailAddresses, webhookUrl)

/**
 * DeadlineCheckUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow
 * by the subscriptionId.
 *
 * @param subscriptionId String
 */
data class DeadlineCheckUnSubscription(val subscriptionId:String)