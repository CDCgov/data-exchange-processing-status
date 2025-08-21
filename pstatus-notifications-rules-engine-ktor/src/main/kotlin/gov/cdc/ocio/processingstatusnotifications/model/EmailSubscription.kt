package gov.cdc.ocio.processingstatusnotifications.model


/**
 * Email Subscription data class which is serialized back and forth
 */
data class EmailSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String?,
    val ruleDescription: String?,
    val mvelCondition: String,
    val emailAddresses: List<String>
)