package gov.cdc.ocio.processingnotifications.model


/**
 * Digest of upload counts subscription definition.
 *
 * @property dataStreamIds List<String>
 * @property dataStreamRoutes List<String>
 * @property jurisdictions List<String>
 * @property cronSchedule String
 * @property emailAddresses List<String>
 * @constructor
 */
class UploadDigestSubscription(
    val dataStreamIds: List<String>,
    val dataStreamRoutes: List<String>,
    val jurisdictions: List<String>,
    val cronSchedule: String,
    val emailAddresses: List<String>
)

/**
 * UploadDigestUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by
 * the subscriptionId.
 *
 * @param subscriptionId String
 */
data class UploadDigestUnSubscription(val subscriptionId:String)