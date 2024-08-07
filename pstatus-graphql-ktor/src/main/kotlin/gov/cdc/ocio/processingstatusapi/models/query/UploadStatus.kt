package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.google.gson.Gson
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.UploadMetadataVerifyStage
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.UploadStage
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.reports.stagereports.SchemaDefinition
import io.ktor.server.util.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


/**
 * Upload status response definition.
 *
 * @property status String?
 * @property percentComplete Float?
 * @property fileName String?
 * @property fileSizeBytes Int?
 * @property bytesUploaded Int?
 * @property uploadId String?
 * @property timeUploadingSec Double?
 * @property issues MutableList<String>?
 * @property timestamp OffsetDateTime?
 */
class UploadStatus {

    var status: String? = null

    var percentComplete: Float? = null

    var fileName: String? = null

    var fileSizeBytes: Long? = null

    var bytesUploaded: Long? = null

    var uploadId: String? = null

    var timeUploadingSec: Double? = null

    var metadata: Map<String, Any>? = null

    var issues: MutableList<String>? = null

    var timestamp: OffsetDateTime? = null

    companion object {

        /**
         * Convenience class method to instantiate an UploadStatus object from a Report list.
         *
         * @param uploadId String
         * @param reports List<Report>
         * @return UploadStatus
         * @throws ContentException
         */
        @Throws(ContentException::class)
        fun createFromReports(uploadId: String, reports: List<ReportDao>): UploadStatus {

            val uploadStatus = UploadStatus()
            var isFailedUpload = false

            // Convert the reports to their schema objects
            val reportsWithSchemaPairs = mutableListOf<Pair<SchemaDefinition, ReportDao>>()
            reports.forEach { report ->
                if (report.contentType != "json")
                    throw ContentException("Content type is not JSON as expected")

                val schemaDefinition = SchemaDefinition.fromJsonString(report.contentAsString)
                reportsWithSchemaPairs.add(Pair(schemaDefinition, report))
            }

            // Sort the reports according to their schema type.
            reportsWithSchemaPairs.sortedBy { it.first }

            reportsWithSchemaPairs.forEach { reportWithSchemaPair ->

                val schemaDefinition = reportWithSchemaPair.first

                val report = reportWithSchemaPair.second
                val stageReportJson = report.contentAsString

                // Attempt to interpret the stage as an upload stage
                when (schemaDefinition) {
                    UploadStage.schemaDefinition -> {
                        val uploadStage = Gson().fromJson(stageReportJson, UploadStage::class.java)
                            ?: throw ContentException("Unable to interpret stage report content as an upload stage")
                        var calculatedPercentComplete = 0F
                        if (uploadStage.size > 0)
                            calculatedPercentComplete = uploadStage.offset.toFloat() / uploadStage.size * 100

                        val isUploadInProgress = (uploadStage.offset < uploadStage.size)
                        val statusMessage = if (isUploadInProgress) "Uploading" else "UploadComplete"
                        val endTimeEpochMillis = if (isUploadInProgress) Date().time/1000 else uploadStage.endTimeEpochMillis

                        if (!isFailedUpload) {
                            uploadStatus.status = statusMessage
                            uploadStatus.fileSizeBytes = uploadStage.size
                            uploadStatus.bytesUploaded = uploadStage.offset
                            uploadStatus.percentComplete = calculatedPercentComplete
                        }
                        uploadStatus.uploadId = uploadId
                        uploadStatus.fileName = uploadStage.filename
                        uploadStatus.timeUploadingSec = (endTimeEpochMillis - uploadStage.startTimeEpochMillis) / 1000.0
                        uploadStatus.metadata = uploadStage.metadata
                        uploadStatus.timestamp = report.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
                    }
                    UploadMetadataVerifyStage.schemaDefinition -> {
                        val metadataVerifyStage = Gson().fromJson(stageReportJson, UploadMetadataVerifyStage::class.java)
                            ?: throw ContentException("Unable to interpret stage report content as a metadata verify stage")
                        val hasIssues = metadataVerifyStage.issues != null && (metadataVerifyStage.issues?.count() ?: 0) > 0
                        if (hasIssues) {
                            metadataVerifyStage.issues?.let { issues ->
                                uploadStatus.status = "FailedMetadata"
                                uploadStatus.fileSizeBytes = null
                                uploadStatus.bytesUploaded = null
                                uploadStatus.percentComplete = null
                                uploadStatus.timeUploadingSec = null
                                uploadStatus.uploadId = uploadId
                                uploadStatus.fileName = metadataVerifyStage.filename
                                uploadStatus.metadata = metadataVerifyStage.metadata
                                uploadStatus.timestamp = report.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
                                if (uploadStatus.issues == null)
                                    uploadStatus.issues = mutableListOf()
                                uploadStatus.issues?.addAll(issues)
                                isFailedUpload = true
                            }
                        } else {
                            uploadStatus.status = "PassedMetadata"
                            uploadStatus.uploadId = uploadId
                            uploadStatus.fileName = metadataVerifyStage.filename
                            uploadStatus.metadata = metadataVerifyStage.metadata
                            uploadStatus.timestamp = report.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
                        }
                    }
                }
            }

            return uploadStatus
        }
    }
}