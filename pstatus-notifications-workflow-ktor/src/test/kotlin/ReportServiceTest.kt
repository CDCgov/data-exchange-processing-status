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
        val failedMetadataVerifyCount = service.countFailedReports("dextesting", "testevent1", "metadata-verify")
        val delayedUploads = service.getDelayedUploads("dextesting", "testevent1")
        val delayedDeliveries = service.getDelayedDeliveries("dextesting", "testevent1")

        assertEquals(0, failedMetadataVerifyCount)
        assertEquals(0, delayedUploads.size)
        assertEquals(0, delayedDeliveries.size)
    }

    @Test
    fun `returns upload failures`() {
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
        repository.reportsCollection.createItem(failedMetadataVerifyReport.id!!, failedMetadataVerifyReport, ReportDao::class.java, null)
        repository.reportsCollection.createItem(successfulMetadataVerifyReport.id!!, successfulMetadataVerifyReport, ReportDao::class.java, null)

        val failedMetadataVerifyCount = service.countFailedReports("dextesting", "testevent1", "metadata-verify")
        assertEquals(1, failedMetadataVerifyCount)
    }
}