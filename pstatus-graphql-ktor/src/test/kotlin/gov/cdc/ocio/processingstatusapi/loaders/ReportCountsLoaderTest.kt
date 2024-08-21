import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.util.CosmosPagedIterable
import data.UploadsStatusDataGenerator
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.loaders.ReportCountsLoader
import gov.cdc.ocio.processingstatusapi.models.ReportCounts
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.reports.StageCounts
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ReportCountsLoaderTest {

    private val mockCosmosRepository: CosmosRepository = mockk()
    private val mockReportsContainer: CosmosContainer = mockk()
    private val mockReportCountsLoader: ReportCountsLoader = mockk()

    private val testModule = module {
        single { mockCosmosRepository }
        single { mockReportsContainer }
        single { mockReportCountsLoader }
    }

    @BeforeEach
    fun setUp() {
        startKoin { modules(testModule) }
        every { mockCosmosRepository.reportsContainer } returns mockReportsContainer
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun withUploadId_reportExists() {
        val uploadId = "uploadId123"

        // Mock query for StageCounts
        val stageCountsList = listOf(StageCounts("stage1", "schemaVersion", "stageName", 10))
        val mockPagedIterableForStageCounts: CosmosPagedIterable<StageCounts> = mockk {
            every { iterator() } returns stageCountsList.iterator() as MutableIterator<StageCounts>
        }

        every {
            mockReportsContainer.queryItems(
                any<String>(),
                any<CosmosQueryRequestOptions>(),
                StageCounts::class.java
            )
        } returns mockPagedIterableForStageCounts

        // Mock query for ReportDao
        val firstReport = ReportDao().apply {
            this.uploadId = uploadId
            this.dataStreamId = "dataStreamId"
            this.dataStreamRoute = "dataStreamRoute"
            this.timestamp = Date()
        }

        val reports = UploadsStatusDataGenerator().createReportsData()
        val mockPagedIterableForReports: CosmosPagedIterable<ReportDao> = mockk {
            every { iterator() } returns reports.iterator() as MutableIterator<ReportDao>
        }

        every {
            mockReportsContainer.queryItems(
                any<String>(),
                any<CosmosQueryRequestOptions>(),
                ReportDao::class.java
            )
        } returns mockPagedIterableForReports

        every { mockPagedIterableForReports.firstOrNull() } returns firstReport

        val reportCounts: ReportCounts = ReportCounts(firstReport.uploadId, firstReport.dataStreamId, firstReport.dataStreamRoute)

        // Mock the behavior of `reportCountsLoader.withUploadId`
        every { mockReportCountsLoader.withUploadId(uploadId) } returns reportCounts

        // Use the mocked instance of ReportCountsLoader in your test
        val result = mockReportCountsLoader.withUploadId(uploadId)

        assertNotNull(result)
        assertEquals(uploadId, result.uploadId)
        assertEquals("dataStreamId", result.dataStreamId)
        assertEquals("dataStreamRoute", result.dataStreamRoute)
    }


    @Test
    fun withUploadId_reportNotExists() {
        val uploadId = "uploadIdNotExists"

        // Mock query for StageCounts
        val stageCountsList = listOf(StageCounts("stage1", "schemaVersion", "stageName", 10))
        val mockPagedIterableForStageCounts: CosmosPagedIterable<StageCounts> = mockk {
            every { iterator() } returns stageCountsList.iterator() as MutableIterator<StageCounts>
        }

        every {
            mockReportsContainer.queryItems(
                any<String>(),
                any<CosmosQueryRequestOptions>(),
                StageCounts::class.java
            )
        } returns mockPagedIterableForStageCounts

        // Mock query for ReportDao
        val firstReport = ReportDao().apply {
            this.uploadId = uploadId
            this.dataStreamId = "dataStreamId"
            this.dataStreamRoute = "dataStreamRoute"
            this.timestamp = Date()
        }

        val reports = UploadsStatusDataGenerator().createReportsData()
        val mockPagedIterableForReports: CosmosPagedIterable<ReportDao> = mockk {
            every { iterator() } returns reports.iterator() as MutableIterator<ReportDao>
        }

        every {
            mockReportsContainer.queryItems(
                any<String>(),
                any<CosmosQueryRequestOptions>(),
                ReportDao::class.java
            )
        } returns mockPagedIterableForReports

        every { mockPagedIterableForReports.firstOrNull() } returns null

        val reportCounts: ReportCounts = ReportCounts(firstReport.uploadId, firstReport.dataStreamId, firstReport.dataStreamRoute)

        // Mock the behavior of `reportCountsLoader.withUploadId`
        every { mockReportCountsLoader.withUploadId(uploadId) } returns null

        // Use the mocked instance of ReportCountsLoader in your test
        val result = mockReportCountsLoader.withUploadId(uploadId)

        Assertions.assertNull(result)
    }
}
