import data.UploadsStatusDataGenerator
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.util.CosmosPagedIterable
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.loaders.UploadStatusLoader
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
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

}
