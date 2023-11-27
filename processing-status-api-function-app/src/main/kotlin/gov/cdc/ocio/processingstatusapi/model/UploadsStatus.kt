package gov.cdc.ocio.processingstatusapi.model

/**
 * Upload status response definition.
 *
 * @property summary PageSummary
 * @property items MutableList<UploadStatus>
 */
data class UploadsStatus(
    var summary: PageSummary = PageSummary(),

    var items: MutableList<UploadStatus> = mutableListOf()
)