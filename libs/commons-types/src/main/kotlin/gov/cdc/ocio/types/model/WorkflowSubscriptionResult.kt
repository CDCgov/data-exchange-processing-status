package gov.cdc.ocio.types.model

import kotlinx.serialization.Serializable


/**
 * The resultant class for subscription of email/webhooks.
 *
 * @param subscriptionId String
 */
@Serializable
data class WorkflowSubscriptionResult(
    var subscriptionId: String? = null
)