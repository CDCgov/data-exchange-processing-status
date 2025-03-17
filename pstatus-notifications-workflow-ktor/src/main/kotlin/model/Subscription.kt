package gov.cdc.ocio.processingnotifications.model


/**
 * Upload errors notification Subscription data class which is serialized back and forth from graphQL to this service.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param cronSchedule String
 * @param emailAddresses List<String>
 */
data class UploadErrorsNotificationSubscription(
    override val dataStreamId: String,
    override val dataStreamRoute: String,
    override val jurisdiction: String,
    override val cronSchedule: String,
    override val emailAddresses: List<String>
) : BaseSubscription(dataStreamId, dataStreamRoute, jurisdiction, cronSchedule, emailAddresses)

/**
 * Upload errors notification unSubscription data class which is serialized back and forth from graphQL to this
 * service.
 *
 * @param subscriptionId String
 */
data class UploadErrorsNotificationUnSubscription(val subscriptionId:String)

/**
 * Data stream top errors notification subscription class  which is serialized back and forth from graphQL to this
 * service.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param cronSchedule String
 * @param emailAddresses List<String>
 */
data class DataStreamTopErrorsNotificationSubscription(
    override val dataStreamId: String,
    override val dataStreamRoute: String,
    override val jurisdiction: String,
    override val cronSchedule: String,
    override val emailAddresses: List<String>
) : BaseSubscription(dataStreamId, dataStreamRoute, jurisdiction, cronSchedule, emailAddresses)

/**
 * Data stream errors notification unSubscription data class which is serialized back and forth  from graphQL to this
 * service.
 *
 * @param subscriptionId String
 */
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId: String)
