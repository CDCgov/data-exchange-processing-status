package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.model.stagereports.SchemaDefinition
import gov.cdc.ocio.processingstatusapi.model.stagereports.UploadStage
import java.util.*

/**
 * Upload status response definition.
 *
 * @property status String?
 * @property percentComplete Float
 * @property fileName String?
 * @property fileSizeBytes Long
 * @property bytesUploaded Long
 * @property tusUploadId String?
 * @property timeUploadingSec Double
 * @property metadata Map<String, Any>?
 * @property timestamp String?
 */
class UploadStatus {

    var status: String? = null

    @SerializedName("percent_complete")
    var percentComplete: Float = 0F

    @SerializedName("filename")
    var fileName: String? = null

    @SerializedName("file_size_bytes")
    var fileSizeBytes: Long = 0

    @SerializedName("bytes_uploaded")
    var bytesUploaded: Long = 0

    @SerializedName("tud_upload_id")
    var tusUploadId: String? = null

    @SerializedName("time_uploading_sec")
    var timeUploadingSec: Double = 0.0

    var metadata: Map<String, Any>? = null

    var timestamp: String? = null

    companion object {

        /**
         * Convenience class method to instantiate an UploadStatus object from a StageReport object.
         *
         * @param stageReport StageReport
         * @return UploadStatus
         * @throws ContentException
         */
        @Throws(ContentException::class)
        fun createFromStageReport(stageReport: StageReport): UploadStatus {

            if (stageReport.contentType != "json")
                throw ContentException("Content type is not JSON as expected")

            val stageReportJson = stageReport.content

            val schemaDefinition = Gson().fromJson(stageReportJson, SchemaDefinition::class.java)

            // Attempt to interpret the stage as an upload stage
            if (schemaDefinition != UploadStage.schemaDefinition)
                throw ContentException("Schema definition of stage report report=$schemaDefinition mismatch from upload=${UploadStage.schemaDefinition}")

            val uploadStage = Gson().fromJson(stageReportJson, UploadStage::class.java)
                    ?: throw ContentException("Unable to interpret stage report content as an upload state")

            var calculatedPercentComplete = 0F
            if (uploadStage.size > 0)
                calculatedPercentComplete = uploadStage.offset.toFloat() / uploadStage.size * 100

            val isUploadInProgress = (uploadStage.offset < uploadStage.size)
            val statusMessage = if (isUploadInProgress) "Uploading" else "UploadComplete"
            val endTimeEpochMillis = if (isUploadInProgress) Date().time/1000 else uploadStage.endTimeEpochMillis
            return UploadStatus().apply {
                status = statusMessage
                tusUploadId = uploadStage.tguid
                fileName = uploadStage.filename
                fileSizeBytes = uploadStage.size
                bytesUploaded = uploadStage.offset
                percentComplete = calculatedPercentComplete
                timeUploadingSec = (endTimeEpochMillis - uploadStage.startTimeEpochMillis) / 1000.0
                metadata = uploadStage.metadata
                timestamp = uploadStage.getTimestamp()
            }
        }
    }
}