package gov.cdc.ocio.processingstatusapi.models.reports

import gov.cdc.ocio.processingstatusapi.models.reports.BaseContent

class MetadataVerifyContent : BaseContent {
    override lateinit var schema_name: String

    override lateinit var schema_version: String

    var filename: String? = null
}