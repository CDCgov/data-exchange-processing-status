package gov.cdc.ocio.processingnotifications.model


/**
 * The POJO class which is used for mapping results from the db
 * @property dataStreamId String
 * @property dataStreamRoute String
 * @property jurisdiction String
 * @constructor
 */
data class UploadDigestResponse(
    var dataStreamId: String = "",
    var dataStreamRoute: String = "",
    var jurisdiction: String = "",
)