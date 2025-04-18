import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.database.couchbase.CouchbaseRepository
import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.database.models.dao.StageInfoDao
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.service.ReportService
import gov.cdc.ocio.types.model.Status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import java.time.Instant
import kotlin.test.assertEquals

@Tag("IntegrationTest")
class ReportServiceTest : KoinTest {
    private val ds = "dextesting"
    private val r = "testevent1"
    private val testCollection = "TestReports"
    private val testModule = module {
        val dbUrl = System.getenv("COUCHBASE_CONNECTION_STRING") ?: "couchbase://localhost"
        val dbUsername = System.getenv("COUCHBASE_USERNAME") ?: "admin"
        val dbPassword = System.getenv("COUCHBASE_PASSWORD") ?: "password"

        single { ReportService() }
        single<ProcessingStatusRepository> { CouchbaseRepository(
            dbUrl,
            dbUsername,
            dbPassword,
            reportsCollectionName = testCollection
        ) }
        single { CouchbaseConfiguration(dbUrl, dbUsername, dbPassword) }
    }
    private val repository by inject<ProcessingStatusRepository>()
    private val service by inject<ReportService>()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        printLogger()
        modules(testModule)
    }

    @BeforeEach
    fun init() {
        repository.createCollection(testCollection)
        Thread.sleep(500) // shouldn't have to do this, but seeing inconsistent behavior without it
    }

    @AfterEach
    fun cleanup() {
        repository.deleteCollection(testCollection)
        stopKoin()
    }

    @Test
    fun `returns zero failures when database is empty`() {
        val failedMetadataVerifyCount = service.countFailedReports(ds, r, StageAction.METADATA_VERIFY, null)
        val delayedUploads = service.getDelayedUploads(ds, r, null)
        val delayedDeliveries = service.getDelayedDeliveries(ds, r, null)

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
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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

        val failedMetadataVerifyCount = service.countFailedReports(ds, r, StageAction.METADATA_VERIFY, null)
        assertEquals(1, failedMetadataVerifyCount)
    }

    @Test
    fun `returns upload failures days interval`() {
        val twoDaysAgo = Instant.now().minusSeconds(172800)
        val failedMetadataVerifyReportOld = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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

        val failedMetadataVerifyCount = service.countFailedReports(ds, r, StageAction.METADATA_VERIFY, 1)
        assertEquals(1, failedMetadataVerifyCount)
    }

    @Test
    fun `returns delayed uploads all time`() {
        val newUploadStartedReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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

        val delayedUploads = service.getDelayedUploads(ds, r, null)

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
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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

        val delayedUploads = service.getDelayedUploads(ds, r, 1)

        assertEquals(1, delayedUploads.size)
        assertEquals(delayedUploads.first(), oldUploadStartedReport.uploadId)
    }

    @Test
    fun `returns failed deliveries`() {
        val failedDeliveryReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = ds,
            dataStreamRoute = r,
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
            dataStreamId = ds,
            dataStreamRoute = r,
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

        val failedDeliveryCount = service.countFailedReports(ds, r, StageAction.FILE_DELIVERY, null)

        assertEquals(1, failedDeliveryCount)
    }

    @Test
    fun `return delayed deliveries all time`() {
        val successfulDeliveryReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = ds,
            dataStreamRoute = r,
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "blob-file-copy", status = Status.SUCCESS)
        )
        val newUndeliveredReport = ReportDao(
            id = "sampleId2",
            uploadId = "uploadId2",
            reportId = "sampleId2",
            dataStreamId = ds,
            dataStreamRoute = r,
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-completed", status = Status.SUCCESS)
        )
        val oldUndeliveredReport = ReportDao(
            id = "sampleId3",
            uploadId = "uploadId3",
            reportId = "sampleId3",
            dataStreamId = ds,
            dataStreamRoute = r,
            dexIngestDateTime = Instant.now().minusSeconds(4000),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now().minusSeconds(4000),
            stageInfo = StageInfoDao(action = "upload-completed", status = Status.SUCCESS)
        )

        listOf(successfulDeliveryReport, newUndeliveredReport, oldUndeliveredReport).forEach {
            repository.reportsCollection.createItem(it.id!!, it, ReportDao::class.java, null)
        }

        val delayedUploads = service.getDelayedDeliveries(ds, r, null)

        assertEquals(1, delayedUploads.size)
        assertEquals(oldUndeliveredReport.uploadId, delayedUploads.first())
    }

    @Test
    fun `return delayed deliveries days interval`() {
        val successfulDeliveryReport = ReportDao(
            id = "sampleId1",
            uploadId = "uploadId1",
            reportId = "sampleId1",
            dataStreamId = ds,
            dataStreamRoute = r,
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "blob-file-copy", status = Status.SUCCESS)
        )
        val newUndeliveredReport = ReportDao(
            id = "sampleId2",
            uploadId = "uploadId2",
            reportId = "sampleId2",
            dataStreamId = ds,
            dataStreamRoute = r,
            dexIngestDateTime = Instant.now(),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now(),
            stageInfo = StageInfoDao(action = "upload-completed", status = Status.SUCCESS)
        )
        val oldUndeliveredReport = ReportDao(
            id = "sampleId3",
            uploadId = "uploadId3",
            reportId = "sampleId3",
            dataStreamId = ds,
            dataStreamRoute = r,
            dexIngestDateTime = Instant.now().minusSeconds(4000),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now().minusSeconds(4000),
            stageInfo = StageInfoDao(action = "upload-completed", status = Status.SUCCESS)
        )
        val reallyOldUndeliveredReport = ReportDao(
            id = "sampleId3",
            uploadId = "uploadId3",
            reportId = "sampleId3",
            dataStreamId = ds,
            dataStreamRoute = r,
            dexIngestDateTime = Instant.now().minusSeconds(172800),
            tags = mapOf("tag1" to "value1", "tag2" to "value2"),
            data = mapOf("dataKey1" to "dataValue1", "dataKey2" to "dataValue2"),
            contentType = "application/json",
            jurisdiction = "jurisdictionXYZ",
            senderId = "sender123",
            dataProducerId="dataProducer123",
            timestamp = Instant.now().minusSeconds(172800),
            stageInfo = StageInfoDao(action = "upload-completed", status = Status.SUCCESS)
        )

        listOf(successfulDeliveryReport, newUndeliveredReport, oldUndeliveredReport).forEach {
            repository.reportsCollection.createItem(it.id!!, it, ReportDao::class.java, null)
        }

        val delayedUploads = service.getDelayedDeliveries(ds, r, 1)

        assertEquals(1, delayedUploads.size)
        assertEquals(oldUndeliveredReport.uploadId, delayedUploads.first())
    }
}