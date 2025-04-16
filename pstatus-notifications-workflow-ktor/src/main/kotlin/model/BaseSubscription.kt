package gov.cdc.ocio.processingnotifications.model

enum class SubscriptionType {
    Email, Webhook
}

/**
 * Base class for subscription
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param cronSchedule String
 * @param emailAddresses List<String>
 */
//open class BaseSubscription(
//    open val dataStreamId: String,
//    open val dataStreamRoute: String,
//    open val jurisdiction: String,
//    open val cronSchedule: String,
//    open val type: SubscriptionType,
//    open val emailAddresses: List<String>,
//    open val webhookUrl: String,
//)
//
//data class Unsubscribe(val subscriptionId: String)

