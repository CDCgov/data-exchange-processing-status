package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.Gson
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

    var percentComplete: Float = 0F

    var fileName: String? = null

    var fileSizeBytes: Long = 0

    var bytesUploaded: Long = 0

    var tusUploadId: String? = null

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
            val endTimeEpochMillis = if (isUploadInProgress) Date().time/1000 else uploadStage.end_time_epoch_millis
            return UploadStatus().apply {
                status = statusMessage
                tusUploadId = uploadStage.tguid
                fileName = uploadStage.filename
                fileSizeBytes = uploadStage.size
                bytesUploaded = uploadStage.offset
                percentComplete = calculatedPercentComplete
                timeUploadingSec = (endTimeEpochMillis - uploadStage.start_time_epoch_millis) / 1000.0
                metadata = uploadStage.metadata
                timestamp = uploadStage.getTimestamp()
            }
        }
    }
}