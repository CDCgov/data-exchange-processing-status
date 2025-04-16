package gov.cdc.ocio.processingnotifications.model

data class Subscription(
    val dataStreamIds: List<String>,
    val dataStreamRoutes: List<String>,
    val jurisdictions: List<String>,
    val sinceDays: Int = 5,
    val cronSchedule: String = "0 0 * * *",
    val emailAddresses: List<String>?,
    val webhookUrl: String?,
)

//open class IntervalSubscription(
//    override val dataStreamId: String,
//    override val dataStreamRoute: String,
//    override val jurisdiction: String,
//    override val cronSchedule: String,
//    override val type: SubscriptionType,
//    override val emailAddresses: List<String>,
//    override val webhookUrl: String,
//    open val daysInterval: Int?
//) : BaseSubscription(dataStreamId, dataStreamRoute, jurisdiction, cronSchedule, type, emailAddresses, webhookUrl)

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
//data class DataStreamTopErrorsNotificationSubscription(
//    override val dataStreamId: String,
//    override val dataStreamRoute: String,
//    override val jurisdiction: String,
//    override val cronSchedule: String,
//    override val emailAddresses: List<String>,
//    override val daysInterval: Int?
//) : IntervalSubscription(dataStreamId, dataStreamRoute, jurisdiction, cronSchedule, emailAddresses, daysInterval)

/**
 * Data stream errors notification unSubscription data class which is serialized back and forth  from graphQL to this
 * service.
 *
 * @param subscriptionId String
 */
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId: String)
