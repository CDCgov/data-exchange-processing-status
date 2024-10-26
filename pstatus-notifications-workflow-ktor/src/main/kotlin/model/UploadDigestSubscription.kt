package gov.cdc.ocio.processingnotifications.model

class UploadDigestSubscription (
    val jurisdictionIds: List<String>,
    val dataStreamIds: List<String>,
    val daysToRun: List<String>,
    val timeToRun: String,
    val deliveryReference: String
    )


/**
 * UploadDigestUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by the subscriptionId
 * @param subscriptionId String
 */
data class UploadDigestUnSubscription(val subscriptionId:String)