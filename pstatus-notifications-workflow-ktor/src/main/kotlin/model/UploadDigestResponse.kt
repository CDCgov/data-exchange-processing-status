package gov.cdc.ocio.processingnotifications.model


/**
 * The POJO class which is used for mapping results from the db.
 *
 * @property dataStreamId String
 * @property dataStreamRoute String
 * @property jurisdiction String
 * @property started Int
 * @property completed Int
 * @property failedDelivery Int
 * @property delivered Int
 * @constructor
 */
data class UploadDigestResponse(
    var dataStreamId: String = "",
    var dataStreamRoute: String = "",
    var jurisdiction: String = "",
    var started: Int = 0,
    var completed: Int = 0,
    var failedDelivery: Int = 0,
    var delivered: Int = 0
)