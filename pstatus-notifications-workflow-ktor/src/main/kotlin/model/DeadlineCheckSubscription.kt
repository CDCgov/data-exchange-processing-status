package gov.cdc.ocio.processingnotifications.model

/**
 * DeadlineCheckUnSubscription data class which is serialized back and forth when we need to unsubscribe the workflow
 * by the subscriptionId.
 *
 * @param subscriptionId String
 */
data class DeadlineCheckUnSubscription(val subscriptionId:String)