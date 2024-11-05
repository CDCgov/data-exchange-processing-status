package gov.cdc.ocio.processingstatusapi.loaders

import data.UploadsStatusDataGenerator
import gov.cdc.ocio.database.cosmos.CosmosCollection
import gov.cdc.ocio.database.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.models.ReportCounts
import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.reports.StageCounts
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class ReportCountsLoaderTest {

    private val mockCosmosRepository: CosmosRepository = mockk()
    private val mockReportsCollection: CosmosCollection = mockk()
    private val mockReportCountsLoader: ReportCountsLoader = mockk()

    private val testModule = module {
        single { mockCosmosRepository }
        single { mockReportsCollection }
        single { mockReportCountsLoader }
    }

    @BeforeEach
    fun setUp() {
        startKoin { modules(testModule) }
        every { mockCosmosRepository.reportsCollection } returns mockReportsCollection
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun withUploadId_reportExists() {
        val uploadId = "uploadId1"

        // Mock query for StageCounts
        val stageCountsList = listOf(StageCounts("stage1", "schemaVersion", "stageName", 10))


        every {
            mockReportsCollection.queryItems(
                any<String>(),
                StageCounts::class.java
            )
        } returns  stageCountsList


        // Mock query for ReportDao
        val firstReport = ReportDao().apply {
            this.uploadId = "uploadId1"
            this.dataStreamId = "dataStream1"
            this.dataStreamRoute = "routeA"
            this.timestamp = Instant.now()
        }
        val reports = UploadsStatusDataGenerator().createReportsData()
        val mockedIterator = mockk<Iterator<ReportDao>>()
        every { mockedIterator.hasNext() } returnsMany listOf(true, true, false) // 2 elements, then end
        every { mockedIterator.next() } returnsMany reports
        val mockedReportList = mockk<List<ReportDao>>()
        every { mockedReportList.iterator() } returns mockedIterator


        every {
            mockReportsCollection.queryItems(
                any<String>(),
                ReportDao::class.java
            )
        } returns mockedReportList

        every { mockedReportList.firstOrNull() } returns firstReport

        val reportCounts: ReportCounts = ReportCounts(firstReport.uploadId, firstReport.dataStreamId, firstReport.dataStreamRoute)

        // Mock the behavior of `reportCountsLoader.withUploadId`
        every { mockReportCountsLoader.withUploadId(uploadId) } returns reportCounts

        // Use the mocked instance of ReportCountsLoader in your test
        val result = mockReportCountsLoader.withUploadId(uploadId)

        assertNotNull(result)
        assertEquals(uploadId, result.uploadId)
        assertEquals("dataStream1", result.dataStreamId)
        assertEquals("routeA", result.dataStreamRoute)
    }


    @Test
    fun withUploadId_reportNotExists() {
        val uploadId = "uploadIdNotExists"

        // Mock query for StageCounts
        val stageCountsList = listOf(StageCounts("stage1", "schemaVersion", "stageName", 10))


        every {
            mockReportsCollection.queryItems(
                any<String>(),
                StageCounts::class.java
            )
        } returns stageCountsList

        // Mock query for ReportDao
        val firstReport = ReportDao().apply {
            this.uploadId = uploadId
            this.dataStreamId = "dataStream1"
            this.dataStreamRoute = "dataStreamRoute"
            this.timestamp = Instant.now()
        }

        val reports = UploadsStatusDataGenerator().createReportsData()
        val mockedIterator = mockk<Iterator<ReportDao>>()
        every { mockedIterator.hasNext() } returnsMany listOf(true, true, false) // 2 elements, then end
        every { mockedIterator.next() } returnsMany reports
        val mockedReportList = mockk<List<ReportDao>>()
        every { mockedReportList.iterator() } returns mockedIterator

        every {
            mockReportsCollection.queryItems(
                any<String>(),
                ReportDao::class.java
            )
        } returns mockedReportList

        every { mockedReportList.firstOrNull() } returns null

        val reportCounts: ReportCounts = ReportCounts(firstReport.uploadId, firstReport.dataStreamId, firstReport.dataStreamRoute)

        // Mock the behavior of `reportCountsLoader.withUploadId`
        every { mockReportCountsLoader.withUploadId(uploadId) } returns null

        // Use the mocked instance of ReportCountsLoader in your test
        val result = mockReportCountsLoader.withUploadId(uploadId)

        Assertions.assertNull(result)
    }
}
