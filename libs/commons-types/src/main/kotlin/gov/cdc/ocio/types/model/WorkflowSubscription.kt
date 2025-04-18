package gov.cdc.ocio.types.model

import kotlinx.serialization.Serializable

@Serializable
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

    constructor() : this(listOf(), listOf(), listOf(), "", NotificationType.EMAIL, 0, listOf(), "")
}