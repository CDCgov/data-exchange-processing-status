package gov.cdc.ocio.processingnotifications.model

/**
 * Upload subscription definition.
 *
 * @property jurisdictionIds List<String>
 * @property dataStreamIds List<String>
 * @property cronSchedule String
 * @property deliveryReference String
 * @constructor
 */
class UploadDigestSubscription(
    val jurisdictionIds: List<String>,
    val dataStreamIds: List<String>,
    val cronSchedule: String,
    val deliveryReference: String
)

/**
 * UploadDigestUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by
 * the subscriptionId.
 *
 * @param subscriptionId String
 */
data class UploadDigestUnSubscription(val subscriptionId:String)