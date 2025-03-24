package gov.cdc.ocio.processingstatusnotifications.model

/**
 * UnSubscription data class which is serialized back and forth when we need to unsubscribe by the subscriptionId
 */
data class Unsubscribe(val subscriptionId: String)