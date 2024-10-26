package gov.cdc.ocio.processingnotifications.model






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