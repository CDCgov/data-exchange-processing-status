package gov.cdc.ocio.processingnotifications.model

enum class WorkflowTaskQueue(private val value: String) {
    TOP_ERRORS("dataStreamTopErrorsNotificationTaskQueue"),
    DEADLINE_CHECK("deadlineCheckNotificationTaskQueue"),
    UPLOAD_DIGEST("uploadDigestCountsTaskQueue")
}