package gov.cdc.ocio.types.model

import kotlinx.serialization.Serializable


/**
 * Represents a subscription to a workflow, providing a way to define scheduled notifications,
 * the type of notification (email or webhook), and the related contact information. This is a sealed
 * class intended to be extended for different specific types of workflow subscriptions.
 *
 * @property cronSchedule Specifies the schedule for the workflow subscription in CRON format.
 * It determines when the subscription workflows are triggered.
 * @property notificationType Defines the type of notification. Possible values are EMAIL or WEBHOOK.
 * @property emailAddresses The list of email addresses to be notified if the notification type is EMAIL.
 * This property is optional and can be null if the notification type is WEBHOOK.
 * @property webhookUrl The URL to send notifications to if the notification type is WEBHOOK.
 * This property is optional and can be null if the notification type is EMAIL.
 */
@Serializable
sealed class WorkflowSubscription {
    abstract val cronSchedule: String
    abstract val notificationType: NotificationType
    abstract val emailAddresses: List<String>?
    abstract val webhookUrl: String?
}