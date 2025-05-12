package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.activity.DataRequest


data class TopErrorsRequest(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val dayInterval : Int
) : DataRequest