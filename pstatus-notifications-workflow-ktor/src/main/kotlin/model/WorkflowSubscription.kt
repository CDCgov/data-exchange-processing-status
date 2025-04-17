package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.types.model.NotificationType

data class WorkflowSubscription(
    val dataStreamIds: List<String>,
    val dataStreamRoutes: List<String>,
    val jurisdictions: List<String>,
    val cronSchedule: String,
    val notificationType: NotificationType,
    val sinceDays: Int = 0,
    val emailAddresses: List<String>?,
    val webhookUrl: String?,
) {
    constructor(
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
        cronSchedule: String,
        notificationType: NotificationType,
        emailAddresses: List<String>?,
        webhookUrl: String?
    ) : this(dataStreamIds, dataStreamRoutes, jurisdictions, cronSchedule, notificationType, 0, emailAddresses, webhookUrl)
}