package gov.cdc.ocio.processingnotifications.model

import java.time.Instant

data class UploadInfo(
    val uploadId: String,
    val filename: String? = null,
    val uploadStartTime: Instant? = null
)