package gov.cdc.ocio.processingnotifications.model


/**
 * The POJO class which is used for mapping results from the db
 * @property id String
 * @property jurisdiction String
 * @property dataStreamId String
 */
data class UploadDigestResponse(
    var id: String="",
    var jurisdiction: String = "",
    var dataStreamId: String=""
)