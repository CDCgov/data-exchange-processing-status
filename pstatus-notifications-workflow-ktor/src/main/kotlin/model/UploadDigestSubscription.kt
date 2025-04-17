package gov.cdc.ocio.processingnotifications.model

/**
 * UploadDigestUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by
 * the subscriptionId.
 *
 * @param subscriptionId String
 */
data class UploadDigestUnSubscription(val subscriptionId:String)