import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.database.couchbase.CouchbaseRepository
import gov.cdc.ocio.database.models.Status
import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.database.models.dao.StageInfoDao
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.service.ReportService
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import java.time.Instant
import kotlin.test.assertEquals

class ReportServiceTest : KoinTest {
    val testModule = module {
        single { ReportService() }
        single<ProcessingStatusRepository> { CouchbaseRepository(
            "couchbase://localhost",
            "admin",
            "password",
            reportsCollectionName = "TestReports"
        ) }
        single { CouchbaseConfiguration("couchbase://localhost", "admin", "password") }
    }
    val repository by inject<ProcessingStatusRepository>()
    val service by inject<ReportService>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger()
        modules(testModule)
    }

    @Before
    fun init() {
        repository.createCollection("TestReports")
        Thread.sleep(500) // shouldn't have to do this, but seeing inconsistent behavior without it
    }

    @After
    fun cleanup() {
        repository.deleteCollection("TestReports")
        stopKoin()
    }

    @Test
    fun `returns zero failures when database is empty`() {
        val failedMetadataVerifyCount = service.countFailedReports("dextesting", "testevent1", "metadata-verify", null)
        val delayedUploads = service.getDelayedUploads("dextesting", "testevent1", null)
        val delayedDeliveries = service.getDelayedDeliveries("dextesting", "testevent1")

        assertEquals(0, failedMetadataVerifyCount)
        assertEquals(0, delayedUploads.size)
        assertEquals(0, delayedDeliveries.size)
    }

    @Test
    fun `returns upload failures all time`() {
        val failedMetadataVerifyReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "metadata-verify", status = Status.FAILURE)
        )
        val successfulMetadataVerifyReport = ReportDao(
            id = "sampleId2",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "metadata-verify", status = Status.SUCCESS)
        )
        listOf(failedMetadataVerifyReport, successfulMetadataVerifyReport).forEach {
            repository.reportsCollection.createItem(it.id!!, it, ReportDao::class.java, null)
        }

        val failedMetadataVerifyCount = service.countFailedReports("dextesting", "testevent1", "metadata-verify", null)
        assertEquals(1, failedMetadataVerifyCount)
    }

    @Test
    fun `returns upload failures days interval`() {
        val twoDaysAgo = Instant.now().minusSeconds(172800)
        val failedMetadataVerifyReportOld = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = twoDaysAgo,
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = twoDaysAgo,
            stageInfo = StageInfoDao(action = "metadata-verify", status = Status.FAILURE)
        )
        val failedMetadataVerifyReportNew = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "metadata-verify", status = Status.FAILURE)
        )

        listOf(failedMetadataVerifyReportOld, failedMetadataVerifyReportNew).forEach {
            repository.reportsCollection.createItem(it.id!!, it, ReportDao::class.java, null)
        }

        val failedMetadataVerifyCount = service.countFailedReports("dextesting", "testevent1", "metadata-verify", 1)
        assertEquals(1, failedMetadataVerifyCount)
    }

    @Test
    fun `returns delayed uploads`() {
        val newUploadStartedReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-started", status = Status.SUCCESS)
        )
        val oldUploadStartedReport = ReportDao(
            id = "sampleId2",
            uploadId = "uploadId2",
            reportId = "sampleId2",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now().minusSeconds(4000), // a little more than an hour old
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-started", status = Status.SUCCESS)
        )
        val oldUploadStartedCompleteReport = ReportDao(
            id = "sampleId3",
            uploadId = "uploadId3",
            reportId = "sampleId3",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now().minusSeconds(4000), // a little more than an hour old
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-started", status = Status.SUCCESS)
        )
        val oldUploadCompletedReport = ReportDao(
            id = "sampleId4",
            uploadId = "uploadId3",
            reportId = "sampleId4",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now().minusSeconds(4000), // a little more than an hour old
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-completed", status = Status.SUCCESS)
        )
        listOf(newUploadStartedReport, oldUploadStartedCompleteReport, oldUploadCompletedReport, oldUploadStartedReport).forEach {
            repository.reportsCollection.createItem(it.id!!, it, ReportDao::class.java, null)
        }

        val delayedUploads = service.getDelayedUploads("dextesting", "testevent1", null)

        assertEquals(1, delayedUploads.size)
        assertEquals(delayedUploads.first(), oldUploadStartedReport.uploadId)
    }

    @Test
    fun `returns delayed uploads days interval`() {
        val twoDaysAgo = Instant.now().minusSeconds(172800)
        val newUploadStartedReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-started", status = Status.SUCCESS)
        )
        val oldUploadStartedReport = ReportDao(
            id = "sampleId2",
            uploadId = "uploadId2",
            reportId = "sampleId2",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now().minusSeconds(4000), // a little more than an hour old
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-started", status = Status.SUCCESS)
        )
        val reallyOldUploadStartedReport = ReportDao(
            id = "sampleId2",
            uploadId = "uploadId2",
            reportId = "sampleId2",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = twoDaysAgo,
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = twoDaysAgo,
            stageInfo = StageInfoDao(action = "upload-started", status = Status.SUCCESS)
        )

        listOf(newUploadStartedReport, oldUploadStartedReport, reallyOldUploadStartedReport).forEach {
            repository.reportsCollection.createItem(it.id!!, it, ReportDao::class.java, null)
        }

        val delayedUploads = service.getDelayedUploads("dextesting", "testevent1", 1)

        assertEquals(1, delayedUploads.size)
        assertEquals(delayedUploads.first(), oldUploadStartedReport.uploadId)
    }

    @Test
    fun `returns failed deliveries`() {
        val failedDeliveryReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "blob-file-copy", status = Status.FAILURE)
        )
        val successfulDeliveryReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = "dextesting",
            dataStreamRoute = "testevent1",
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "blob-file-copy", status = Status.FAILURE)
        )

        listOf(failedDeliveryReport, successfulDeliveryReport).forEach {
            repository.reportsCollection.createItem(it.id!!, it, ReportDao::class.java, null)
        }

        val failedDeliveryCount = service.countFailedReports("dextesting", "testevent1", "blob-file-copy", null)

        assertEquals(1, failedDeliveryCount)
    }
}