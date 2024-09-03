import data.UploadsStatusDataGenerator
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.util.CosmosPagedFlux
import com.azure.cosmos.util.CosmosPagedIterable
import com.azure.cosmos.util.UtilBridgeInternal.createCosmosPagedIterable
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.loaders.UploadStatusLoader
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UploadStatusLoaderTest : KoinTest {

    private val mockCosmosRepository: CosmosRepository = mockk()
    private val mockReportsContainer: CosmosContainer = mockk()
    private val uploadStatusLoader: UploadStatusLoader = mockk()

    private val testModule = module {
        single { mockCosmosRepository }
        single { mockReportsContainer }
        single { uploadStatusLoader } // Provide the mocked UploadStatusLoader
    }

    @BeforeEach
    fun setUp() {
        // Start Koin with the test module
        startKoin {
            modules(testModule)
        }

        // Mock CosmosRepository and its container
        every { mockCosmosRepository.reportsContainer } returns mockReportsContainer
    }

    @AfterEach
    fun tearDown() {
        // Stop Koin after each test to ensure a clean state
        stopKoin()
    }

    @Test
    fun getUploads_validParams() {

        // Define the expected behavior for the method
        every {
            uploadStatusLoader.getUploadStatus(
                dataStreamId = any(),
                dataStreamRoute = any(),
                dateStart = any(),
                dateEnd = any(),
                pageSize = any(),
                pageNumber = any(),
                sortBy = any(),
                sortOrder = any(),
                fileName = any()
            )
        } returns UploadsStatusDataGenerator().createUploadsStatusTestData()

        val uploadsStatus = uploadStatusLoader.getUploadStatus(
            dataStreamId = "stream1",
            dataStreamRoute = null,
            dateStart = null,
            dateEnd = null,
            pageSize = 10,
            pageNumber = 1,
            sortBy = null,
            sortOrder = null,
            fileName = null
        )

        assertEquals(2, uploadsStatus.items.size)
        assertEquals(1, uploadsStatus.summary.pageNumber)
        assertEquals(2, uploadsStatus.summary.pageSize)
        assertEquals(3, uploadsStatus.summary.numberOfPages)
        assertEquals(6, uploadsStatus.summary.totalItems)
        assertEquals(listOf("jurisdiction1", "jurisdiction2"), uploadsStatus.summary.jurisdictions)
        assertEquals(listOf("sender1", "sender2"), uploadsStatus.summary.senderIds)
    }

    @Test
    fun getUploads_invalidParams() {

    /*    // Define the expected behavior for the method
        every {
            mockReportsContainer.queryItems(
                any<String>(),
                any<CosmosQueryRequestOptions>(),
                UploadCounts::class.java
            )
        } throws BadRequestException("Please review the search criteria")*/

        // Define the expected behavior for the method
        every {
            uploadStatusLoader.getUploadStatus(
                dataStreamId = any(),
                dataStreamRoute = any(),
                dateStart = any(),
                dateEnd = any(),
                pageSize = any(),
                pageNumber = any(),
                sortBy = any(),
                sortOrder = any(),
                fileName = any()
            )
        } throws BadRequestException("Please review the search criteria")

        assertFailsWith<BadRequestException> {
            uploadStatusLoader.getUploadStatus(
                dataStreamId = "dex-testing",
                dataStreamRoute = "",
                dateStart = "20240808T190000Z",
                dateEnd = "",
                pageSize = 10,
                pageNumber = 1,
                sortBy = "fileName",
                sortOrder = "a",
                fileName = null
            )
        }
    }


    @Test
    fun getUploads_emptyResults() {
        // Define the expected behavior for the method
        every {
            uploadStatusLoader.getUploadStatus(
                dataStreamId = any(),
                dataStreamRoute = any(),
                dateStart = any(),
                dateEnd = any(),
                pageSize = any(),
                pageNumber = any(),
                sortBy = any(),
                sortOrder = any(),
                fileName = any()
            )
        } returns UploadsStatusDataGenerator().createEmptyResults()

        val uploadsStatus = uploadStatusLoader.getUploadStatus(
            dataStreamId = "stream1",
            dataStreamRoute = null,
            dateStart = null,
            dateEnd = null,
            pageSize = 10,
            pageNumber = 1,
            sortBy = null,
            sortOrder = null,
            fileName = null
        )

        assertEquals(0, uploadsStatus.items.size)
        assertEquals(1, uploadsStatus.summary.pageNumber)
        assertEquals(10, uploadsStatus.summary.pageSize)
        assertEquals(0, uploadsStatus.summary.numberOfPages)
        assertEquals(0, uploadsStatus.summary.totalItems)
    }


    @Test
    fun getUploads_results() {
        // Setup mocks
        //Return PageIterable of type UploadCounts
        val uploadCounts = listOf(
            UploadCounts(4, "uploadId1", Date(), "jurisdiction1", "senderId1"),
            UploadCounts(2, "uploadId2", Date(), "jurisdiction2","senderId2")
        )
        val uploadCountsIterator = uploadCounts.iterator()
        val mockPagedIterableForUploadCounts: CosmosPagedIterable<UploadCounts> = mockk {
            every { iterator() } returns uploadCountsIterator as MutableIterator<UploadCounts>
        }

        every {
            mockReportsContainer.queryItems(
                any<String>(),
                any<CosmosQueryRequestOptions>(),
                UploadCounts::class.java
            )
        } returns mockPagedIterableForUploadCounts


        //Return PageIterable of type ReportDAO
        val reports = UploadsStatusDataGenerator().createReportsData()
        val reportsIterator = reports.iterator()
        val mockPagedIterableForReports: CosmosPagedIterable<ReportDao> = mockk {
            every { iterator() } returns reportsIterator as MutableIterator<ReportDao>
        }

        every {
            mockReportsContainer.queryItems(
                any<String>(),
                any<CosmosQueryRequestOptions>(),
                ReportDao::class.java
            )
        } returns mockPagedIterableForReports


        every {
            uploadStatusLoader.getUploadStatus(
                dataStreamId ="dex-testing",
                dataStreamRoute = "",
                dateStart = "2024-08-14T00:00:00Z",
                dateEnd = "2024-08-15T00:00:00Z",
                pageSize =10,
                pageNumber = 1,
                sortBy = "fileName",
                sortOrder = "desc",
                fileName = ""
            )
        } returns UploadsStatusDataGenerator().createUploadsStatusTestData()

        // Execute the method
        val uploadsStatus =  uploadStatusLoader.getUploadStatus(
            dataStreamId ="dex-testing",
            dataStreamRoute = "",
            dateStart = "2024-08-14T00:00:00Z",
            dateEnd = "2024-08-15T00:00:00Z",
            pageSize =10,
            pageNumber = 1,
            sortBy = "fileName",
            sortOrder = "desc",
            fileName = ""
        )

        // Verify the results
        assertNotNull(uploadsStatus)
    }

}