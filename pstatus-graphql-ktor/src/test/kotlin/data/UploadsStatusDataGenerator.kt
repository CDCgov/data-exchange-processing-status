package data

import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import gov.cdc.ocio.processingstatusapi.models.query.UploadStatus
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset

// Define the class to create sample data
class UploadsStatusDataGenerator {

    // Method to create and return an instance of UploadsStatus
    fun createUploadsStatusTestData(): UploadsStatus {
        // Create a sample PageSummary
        val samplePageSummary = PageSummary(
            pageNumber = 1,
            numberOfPages = 3,
            pageSize = 2,
            totalItems = 6,
            senderIds = listOf("sender1", "sender2"),
            jurisdictions = listOf("jurisdiction1", "jurisdiction2")
        )

        // Create sample UploadStatus items
        val sampleUploadStatuses = mutableListOf(
            UploadStatus().apply {
                status = "Completed"
                percentComplete = 100F
                fileName = "document1.pdf"
                fileSizeBytes = 2048L
                bytesUploaded = 2048L
                uploadId = "upload1"
                timeUploadingSec = 45.0
                issues = mutableListOf()
                timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
                senderId = "sender1"
                jurisdiction = "jurisdiction1"
            },
            UploadStatus().apply {
                status = "InProgress"
                percentComplete = 75F
                fileName = "image1.png"
                fileSizeBytes = 1024L
                bytesUploaded = 768L
                uploadId = "upload2"
                timeUploadingSec = 30.0
                issues = mutableListOf("Issue with resolution")
                timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
                senderId = "sender2"
                jurisdiction = "jurisdiction2"
            }
        )

        // Create and return an instance of UploadsStatus with the sample data
        return UploadsStatus(
            summary = samplePageSummary,
            items = sampleUploadStatuses
        )
    }
}