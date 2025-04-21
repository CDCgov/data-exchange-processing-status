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
    // secondary constructor for workflows that don't factor in a day interval, such as deadline check.
    constructor(
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
        cronSchedule: String,
        notificationType: NotificationType,
        emailAddresses: List<String>?,
        webhookUrl: String?
    ) : this(dataStreamIds, dataStreamRoutes, jurisdictions, cronSchedule, notificationType, 0, emailAddresses, webhookUrl)

    // default constructor to make temporal serialization happy
    constructor() : this(listOf(), listOf(), listOf(), "", NotificationType.EMAIL, 0, listOf(), "")
}