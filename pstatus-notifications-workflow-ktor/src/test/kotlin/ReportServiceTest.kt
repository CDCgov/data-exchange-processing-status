import gov.cdc.ocio.database.couchbase.CouchbaseConfiguration
import gov.cdc.ocio.database.couchbase.CouchbaseRepository
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingnotifications.service.ReportService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
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
        runBlocking {
            repository.createCollection("TestReports")
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            repository.deleteCollection("TestReports")
        }
    }

    @Test
    fun `returns zero failures when database empty`() {
        val failedCount = service.countFailedReports("dextesting", "testevent1", "metadata-verify")
        assertEquals(0, failedCount)
    }
}