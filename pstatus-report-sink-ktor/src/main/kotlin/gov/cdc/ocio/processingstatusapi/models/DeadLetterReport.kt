package gov.cdc.ocio.processingstatusapi.models


import com.google.gson.annotations.SerializedName


/**
 * Dead-LetterReport when there is missing fields or malformed data.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dispositionType DispositionType?
 * @property timestamp Date
 * @property contentType String?
 * @property content String?
 * @property deadLetterReasons List<String>
 */
  class ReportDeadLetter : Report() {

   @SerializedName("disposition_type")
    var dispositionType: String? = null

    var deadLetterReasons: List<String>? = null

    var validationSchemas: List<String>? = null
}
