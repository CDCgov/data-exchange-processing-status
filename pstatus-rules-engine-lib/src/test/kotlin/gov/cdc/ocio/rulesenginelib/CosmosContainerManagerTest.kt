package gov.cdc.ocio.rulesenginelib

import com.azure.cosmos.*
import com.azure.cosmos.models.*
import gov.cdc.ocio.rulesenginelib.gov.cdc.ocio.rulesenginelib.cosmos.CosmosContainerManager
import io.mockk.*
import org.testng.Assert.assertNotNull
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test



class CosmosContainerManagerTest {

    private lateinit var mockCosmosClient: CosmosClient
    private lateinit var mockDatabase: CosmosDatabase
    private lateinit var mockContainer: CosmosContainer
    private lateinit var mockContainerResponse: CosmosContainerResponse

    @BeforeTest
    fun setup() {
        mockCosmosClient = mockk(relaxed = true)
        mockDatabase = mockk(relaxed = true)
        mockContainer = mockk(relaxed = true)
        mockContainerResponse = mockk(relaxed = true)

        // Mocking the database creation response
        val databaseResponse = mockk<CosmosDatabaseResponse>()
        every { databaseResponse.properties.id } returns "RulesEngine"
        every { mockCosmosClient.createDatabaseIfNotExists("RulesEngine") } returns databaseResponse
        every { mockCosmosClient.getDatabase("RulesEngine") } returns mockDatabase

        // Mocking the container creation
        every { mockDatabase.createContainerIfNotExists(any<CosmosContainerProperties>(), any()) } returns mockContainerResponse
        every { mockDatabase.getContainer("Rules") } returns mockContainer
    }

    @Test
    fun `createDatabaseIfNotExists should return database instance when creation is successful`() {
        val result = CosmosContainerManager.createDatabaseIfNotExists(mockCosmosClient, "RulesEngine")

        assertNotNull(result)
        verify { mockCosmosClient.createDatabaseIfNotExists("RulesEngine") }
        verify { mockCosmosClient.getDatabase("RulesEngine") }
    }

    @Test
    fun `initDatabaseContainer should initialize a container successfully`() {
       val uri= "https://ocio-ede-dev-processingstatus-test-db.documents.azure.com:443/"
       val containerName = "Rules"
       val partitionKey = "/ruleId"
       val validAuthKey = ""  // get from portal
       val container = CosmosContainerManager.initDatabaseContainer(uri, validAuthKey, containerName, partitionKey)

        assertNotNull(container)

    }
}