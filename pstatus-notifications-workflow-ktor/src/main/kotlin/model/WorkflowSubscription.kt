package gov.cdc.ocio.processingnotifications.model

data class WorkflowSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String,
    val cronSchedule: String,
    val daysInterval: Int?,
    val emailAddresses: List<String>?,
    val webhookUrl: String?,
) {
    constructor(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        cronSchedule: String,
        emailAddresses: List<String>?,
        webhookUrl: String?
    ) : this(dataStreamId, dataStreamRoute, jurisdiction, cronSchedule, 0, emailAddresses, webhookUrl)
}