package gov.cdc.ocio.processingnotifications.model

/**
 * Base class for subscription
 *  @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param daysToRun List<String>
 * @param timeToRun String
 * @param deliveryReference String
 */
open class BaseSubscription(open val dataStreamId: String,
                            open val dataStreamRoute: String,
                            open val jurisdiction: String,
                            open val daysToRun: List<String>,
                            open val timeToRun: String,
                            open val deliveryReference: String) {
}

/**
 * DeadlineCheckSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by the subscriptionId
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
 * DeadlineCheckUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by the subscriptionId
 * @param subscriptionId String
 */
data class DeadlineCheckUnSubscription(val subscriptionId:String)

/**
 * The resultant class for subscription of email/webhooks
 * @param subscriptionId String
 * @param message String
 * @param deliveryReference String
 */
data class WorkflowSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var deliveryReference:String
)

/**
 * Upload errors notification Subscription data class which is serialized back and forth from graphQL to this service
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param daysToRun List<String>
 * @param timeToRun String
 * @param deliveryReference String
 * daysToRun:["Mon","Tue","Wed"]
 * timeToRun:"45 16 * *" - this should be the format
 */
data class UploadErrorsNotificationSubscription( override val dataStreamId: String,
                                                  override val dataStreamRoute: String,
                                                  override val jurisdiction: String,
                                                  override val daysToRun: List<String>,
                                                  override val timeToRun: String,
                                                  override val deliveryReference: String) : BaseSubscription(dataStreamId, dataStreamRoute ,jurisdiction, daysToRun, timeToRun, deliveryReference )


/**
 * Upload errors notification unSubscription data class which is serialized back and forth  from graphQL to this service
 * @param subscriptionId String
 */
data class UploadErrorsNotificationUnSubscription(val subscriptionId:String)

/** Data stream top errors notification subscription class  which is serialized back and forth from graphQL to this service
* @param dataStreamId String
* @param dataStreamRoute String
* @param jurisdiction String
* @param daysToRun List<String>
* @param timeToRun String
* @param deliveryReference String
* daysToRun:["Mon","Tue","Wed"]
* timeToRun:"45 16 * *" - this should be the format
 */
data class DataStreamTopErrorsNotificationSubscription( override val dataStreamId: String,
                                                 override val dataStreamRoute: String,
                                                 override val jurisdiction: String,
                                                 override val daysToRun: List<String>,
                                                 override val timeToRun: String,
                                                 override val deliveryReference: String) : BaseSubscription(dataStreamId, dataStreamRoute ,jurisdiction, daysToRun, timeToRun, deliveryReference )

/**
 * Data stream errors notification unSubscription data class which is serialized back and forth  from graphQL to this service
 * @param subscriptionId String
 */
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId:String)

/**
 * Get Cron expression based on the daysToRun and timeToRun parameters
 * @param daysToRun List<String>
 *
 */
fun getCronExpression(daysToRun: List<String>, timeToRun: String):String{
    val daysToRunInStr =daysToRun.joinToString(separator = ",")
    val cronExpression= "$timeToRun $daysToRunInStr"
    return cronExpression
}