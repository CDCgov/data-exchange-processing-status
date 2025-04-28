package gov.cdc.ocio.types.model

import kotlinx.serialization.Serializable


/**
 * Represents a workflow subscription specific to data streams, extending the generic
 * workflow subscription functionality. This class is tailored to provide additional
 * properties and configurations for managing data streams within the workflow.
 *
 * @property dataStreamIds A list of data stream IDs that are relevant to this subscription.
 * Each ID identifies a specific data stream being tracked or monitored.
 * @property dataStreamRoutes A list of routes associated with the data streams, providing
 * context or pathways through which data flows for this subscription.
 * @property jurisdictions A list of jurisdictions associated with the subscription, indicating
 * the regulatory or geographical areas that the subscription applies to.
 * @property sinceDays An integer representing the number of days from which the data streams
 * should be monitored or considered, allowing for historical or recent data tracking.
 */
@Serializable
data class WorkflowSubscriptionForDataStreams(
    override val cronSchedule: String,
    override val notificationType: NotificationType,
    override val emailAddresses: List<String>?,
    override val webhookUrl: String?,
    val dataStreamIds: List<String>,
    val dataStreamRoutes: List<String>,
    val jurisdictions: List<String>,
    val sinceDays: Int
) : WorkflowSubscription()