package gov.cdc.ocio.processingstatusapi.models.reports

import gov.cdc.ocio.processingstatusapi.models.reports.BaseContent

class UploadStatusContent: BaseContent {
    override lateinit var schema_name: String

    override lateinit var schema_version: String

    var offset: Int? = null

    var size: Int? = null
}