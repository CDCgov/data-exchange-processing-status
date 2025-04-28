package gov.cdc.ocio.types.model

import kotlinx.serialization.Serializable


/**
 * Represents a subscription to a workflow that involves a time-relative parameter for data retrieval,
 * specifically the number of days in the past from which data is included.
 *
 * This class extends the `WorkflowSubscription` base class by including the `sinceDays` property,
 * which allows the subscription to retrieve data starting from a specific number of days ago
 * relative to the current date. All other fields define the core parameters required to schedule
 * and notify regarding the workflow subscription.
 *
 * @property sinceDays Specifies the number of days in the past to include for the data stream.
 * It determines how far back the subscription retrieves data from the associated data streams.
 */
@Serializable
data class WorkflowSubscriptionWithSinceDays(
    override val dataStreamIds: List<String>,
    override val dataStreamRoutes: List<String>,
    override val jurisdictions: List<String>,
    override val cronSchedule: String,
    override val notificationType: NotificationType,
    override val emailAddresses: List<String>?,
    override val webhookUrl: String?,
    val sinceDays: Int
) : WorkflowSubscription()