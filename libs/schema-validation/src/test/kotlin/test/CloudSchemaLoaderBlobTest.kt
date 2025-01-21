import gov.cdc.ocio.reportschemavalidator.loaders.CloudSchemaLoader
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.schema.BlobStorageSchemaClient
import org.mockito.Mockito.mock
import org.mockito.Mockito.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class CloudSchemaLoaderBlobTest {

    @Test
    fun `test load schema file from Blob Storage`() {
        // Arrange
        val mockBlobClient = mock(BlobStorageSchemaClient::class.java)
        val fileName = "example-schema.json"
        val mockContent = "mock data from Blob Storage"
        `when`(mockBlobClient.getSchemaFile(fileName)).thenReturn(mockContent)

        val config = mapOf(
            "REPORT_SCHEMA_BLOB_CONNECTION_STR" to "fake-connection-string",
            "REPORT_SCHEMA_BLOB_CONTAINER" to "test-container"
        )
        val loader = CloudSchemaLoader("blob_storage", config)

        // Inject mock client via reflection
        // Suppress Fortify warning: This use of setAccessible is restricted to test code and is necessary for injecting mocks.
        // the use of setAccessible(true) is restricted to testing (and is safe)
        val storageClientField = loader.javaClass.getDeclaredField("storageClient")
        storageClientField.isAccessible = true
        storageClientField.set(loader, mockBlobClient)

        // Act
        val schemaFile: SchemaFile = loader.loadSchemaFile(fileName)

        // Assert
        assertNotNull(schemaFile.content)
        val actualContent = schemaFile.content
        assertEquals(mockContent, actualContent)
        assertEquals(fileName, schemaFile.fileName)
        verify(mockBlobClient, times(1)).getSchemaFile(fileName)
    }
}
