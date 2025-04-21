package gov.cdc.ocio.processingnotifications.model

/**
 * Data stream errors notification unSubscription data class which is serialized back and forth  from graphQL to this
 * service.
 *
 * @param subscriptionId String
 */
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId: String)
