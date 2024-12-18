package data

import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import gov.cdc.ocio.processingstatusapi.models.query.UploadStatus
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import java.time.Instant
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
            senderIds = mutableListOf("sender1", "sender2"),
            jurisdictions = mutableListOf("jurisdiction1", "jurisdiction2")
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
                dataProducerId="dataProducer1"
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
                dataProducerId="dataProducer2"
                jurisdiction = "jurisdiction2"
            }
        )

        // Create and return an instance of UploadsStatus with the sample data
        return UploadsStatus(
            summary = samplePageSummary,
            items = sampleUploadStatuses
        )
    }

    fun createEmptyResults():UploadsStatus {

        // Create and return an instance of UploadsStatus with the sample data
        return UploadsStatus(
                items = mutableListOf(),
                summary = PageSummary(
                    pageNumber = 1,
                    pageSize = 10,
                    numberOfPages = 0,
                    totalItems = 0,
                    jurisdictions = mutableListOf(),
                    senderIds = mutableListOf()
                )
        )
    }

    fun createReportsData():List<ReportDao>{
        val reports = listOf(
            ReportDao(
                id = "sampleId1",
                uploadId = "uploadId1",
                reportId = "sampleId1",
                dataStreamId = "dataStream1",
                dataStreamRoute = "routeA",
                dexIngestDateTime = Instant.now(),
                tags = mapOf("tag1" to "value1", "tag2" to "value2"),
                data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
                contentType = "application/json",
                jurisdiction = "jurisdictionXYZ",
                senderId = "sender123",
                dataProducerId="dataProducer123",
                timestamp = Instant.now(),
            ),
            ReportDao(
                id = "sampleId2",
                uploadId = "uploadId2",
                reportId = "sampleId2",
                dataStreamId = "dataStream2",
                dataStreamRoute = "routeB",
                dexIngestDateTime = Instant.now(), // current date and time
                tags = mapOf("tag1" to "value1", "tag2" to "value2"),
                data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
                contentType = "application/json",
                jurisdiction = "jurisdictionXYZ",
                senderId = "sender123",
                dataProducerId="dataProducer123",
                timestamp = Instant.now(), // current date and time
            ), // fill with actual fields  // fill with actual fields
        )

        return reports
    }
}