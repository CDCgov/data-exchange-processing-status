package gov.cdc.ocio.processingnotifications.model

enum class WorkflowType(private val value: String) {
    UPLOAD_ERROR_SUMMARY("upload-error-summary"),
    UPLOAD_DEADLINE_CHECK("upload-deadline-check"),
    UPLOAD_DIGEST("upload-digest")
}