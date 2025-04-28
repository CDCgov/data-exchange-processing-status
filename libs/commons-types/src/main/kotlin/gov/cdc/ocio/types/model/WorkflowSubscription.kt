package gov.cdc.ocio.types.model

import kotlinx.serialization.Serializable


/**
 * Represents a subscription to a workflow, defining parameters required for scheduling and notifications.
 *
 * The `WorkflowSubscription` serves as a base class for specific types of workflow subscriptions.
 * Implementations of this class can define additional parameters as needed.
 *
 * @property dataStreamIds List of unique identifiers for data streams associated with the subscription.
 * @property dataStreamRoutes List of routes for data streams associated with the subscription.
 * @property jurisdictions List of jurisdictions applicable to the subscription.
 * @property cronSchedule Cron expression defining the schedule for the subscription.
 * @property notificationType Type of notification to be used (e.g., EMAIL or WEBHOOK).
 * @property emailAddresses Optional list of email addresses to receive notifications if the notification type is EMAIL.
 * @property webhookUrl Optional URL to send notifications to if the notification type is WEBHOOK.
 */
@Serializable
sealed class WorkflowSubscription {
    abstract val dataStreamIds: List<String>
    abstract val dataStreamRoutes: List<String>
    abstract val jurisdictions: List<String>
    abstract val cronSchedule: String
    abstract val notificationType: NotificationType
    abstract val emailAddresses: List<String>?
    abstract val webhookUrl: String?
}