package gov.cdc.ocio.processingnotifications.model

open class BaseSubscription(open val dataStreamId: String,
                            open val dataStreamRoute: String,
                            open val jurisdiction: String,
                            open val daysToRun: List<String>,
                            open val timeToRun: String,
                            open val deliveryReference: String) {
}


data class DeadlineCheckSubscription(
    override val dataStreamId: String,
    override val dataStreamRoute: String,
    override val jurisdiction: String,
    override val daysToRun: List<String>,
    override val timeToRun: String,
    override val deliveryReference: String)
    : BaseSubscription(dataStreamId, dataStreamRoute ,jurisdiction, daysToRun, timeToRun, deliveryReference )




/**
 * DeadlineCheckUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by the subscriptionId
 */
data class DeadlineCheckUnSubscription(val subscriptionId:String)

/**
 * The resultant class for subscription of email/webhooks
 */
data class WorkflowSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var deliveryReference:String
)

/**
 * DeadlineCheck Subscription data class which is serialized back and forth
 */
data class UploadErrorsNotificationSubscription( override val dataStreamId: String,
                                                  override val dataStreamRoute: String,
                                                  override val jurisdiction: String,
                                                  override val daysToRun: List<String>,
                                                  override val timeToRun: String,
                                                  override val deliveryReference: String) : BaseSubscription(dataStreamId, dataStreamRoute ,jurisdiction, daysToRun, timeToRun, deliveryReference )


/**
 * DeadlineCheck Subscription data class which is serialized back and forth
 * daysToRun:["Mon","Tue","Wed"]
 * timeToRun:"45 16 * *" - this should be the format
 */
data class UploadErrorsNotificationUnSubscription(val subscriptionId:String)

fun getCronExpression(daysToRun: List<String>, timeToRun: String):String{
    val daysToRunInStr =daysToRun.joinToString(separator = ",")
    val cronExpression= "$timeToRun $daysToRunInStr"
    return cronExpression
}