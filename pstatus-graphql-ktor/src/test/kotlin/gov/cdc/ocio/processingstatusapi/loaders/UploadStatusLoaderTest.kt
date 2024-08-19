package gov.cdc.ocio.processingstatusapi.loaders

import data.UploadsStatusDataGenerator
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.util.CosmosPagedIterable
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UploadStatusLoaderTest : KoinTest {

    private val mockCosmosRepository: CosmosRepository = mockk()
    private val mockReportsContainer: CosmosContainer = mockk()
    private val uploadStatusLoader: UploadStatusLoader = mockk()

    private val testModule = module {
        single { CosmosRepository("", "", "", "") }
        single { mockReportsContainer }
        single { UploadStatusLoader() } // Ensure UploadStatusLoader is provided
    }


    @BeforeEach
    fun setUp() {
        // Start Koin with a test module
        startKoin {
            modules(testModule)
        }

        // Mock CosmosRepository and its container
        every { mockCosmosRepository.reportsContainer } returns mockReportsContainer

    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `GetUploads_ValidParams`() {
        val mockCounts = listOf(
            UploadCounts(4, "uploadId1", Date(), "jurisdiction1", "senderId1"),
            UploadCounts(2, "uploadId2", Date(), "jurisdiction2","senderId2")
        )

        // Create a custom MutableIterator
        val customIterator = object : MutableIterator<UploadCounts> {
            private val iterator = mockCounts.iterator()

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): UploadCounts = iterator.next()

            override fun remove() {
                iterator.remove()
            }
        }

        val mockPagedIterable: CosmosPagedIterable<UploadCounts> = mockk {
            every { iterator() } returns customIterator
        }

        // Use explicit argument matchers and type-safe method calls
        every {
            mockReportsContainer.queryItems(
                any<String>(), // Query string
                any<CosmosQueryRequestOptions>(), // Request options
                UploadCounts::class.java // Result class type
            )
        } returns mockPagedIterable

       // Instantiate the SampleDataGenerator
        val dataGenerator = UploadsStatusDataGenerator()

        // Generate sample UploadsStatus
        val uploadsStatus = dataGenerator.createUploadsStatusTestData()

        assertEquals(2, uploadsStatus.items.size)
        assertEquals(1, uploadsStatus.summary.pageNumber)
        assertEquals(2, uploadsStatus.summary.pageSize)
        assertEquals(3, uploadsStatus.summary.numberOfPages)
        assertEquals(6, uploadsStatus.summary.totalItems)
        assertEquals(listOf("jurisdiction1", "jurisdiction2"), uploadsStatus.summary.jurisdictions)
        assertEquals(listOf("sender1", "sender2"), uploadsStatus.summary.senderIds)
    }

    @Test
    fun `GetUploads_InvalidParams`() {

        every {
            mockReportsContainer.queryItems(
                any<String>(), // Query string
                any<CosmosQueryRequestOptions>(), // Request options
                ReportDao::class.java // Result class type
            )
        } throws BadRequestException("Invalid query")


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

/*    @Test
    fun `should handle empty results gracefully`() {
        every {
            mockReportsContainer.queryItems(
                any<String>(), // Query string
                any<CosmosQueryRequestOptions>(), // Request options
                UploadCounts::class.java // Result class type
            )
        } returns emptyList<UploadCounts>().asSequence().asIterable() as CosmosPagedIterable<UploadCounts>?

        val uploadsStatus = uploadStatusLoader.getUploadStatus(
            dataStreamId = "stream1",
            dataStreamRoute = null,
            dateStart = null,
            dateEnd = null,
            pageSize = 10,
            pageNumber = 1,
            sortBy = null,
            sortOrder = null,
            fileName = null,
            status = null
        )

        assertEquals(0, uploadsStatus.items.size)
        assertEquals(1, uploadsStatus.summary.pageNumber)
        assertEquals(10, uploadsStatus.summary.pageSize)
        assertEquals(0, uploadsStatus.summary.numberOfPages)
        assertEquals(0, uploadsStatus.summary.totalItems)
    }*/
}

private fun <T> Iterator<T>.remove() {
    TODO("Not yet implemented")
}
