
package test


import gov.cdc.ocio.reportschemavalidator.loaders.CloudSchemaLoader
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.schema.S3SchemaStorageClient
import org.mockito.Mockito.mock
import org.mockito.Mockito.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CloudSchemaLoaderS3Test  {

    @Test
    fun `test load schema file from S3`() {
        // Arrange
        val mockS3Client = mock(S3SchemaStorageClient::class.java)
        val fileName = "example-schema.json"
        val mockContent = "mock data from S3"
        val mockInputStream = mockContent.byteInputStream()
        `when`(mockS3Client.getSchemaFile(fileName)).thenReturn(mockInputStream)

        val config = mapOf(
            "REPORT_SCHEMA_S3_BUCKET" to "test-bucket",
            "REPORT_SCHEMA_S3_REGION" to "us-east-1"
        )
        val loader = CloudSchemaLoader("s3", config)

        // Inject mock client via reflection
        // Suppress Fortify warning: This use of setAccessible is restricted to test code and is necessary for injecting mocks.
        // the use of setAccessible(true) is restricted to testing (and is safe)
        val storageClientField = loader.javaClass.getDeclaredField("storageClient")
        storageClientField.isAccessible = true
        storageClientField.set(loader, mockS3Client)

        // Act
        val schemaFile: SchemaFile = loader.loadSchemaFile(fileName)

        // Assert
        assertNotNull(schemaFile.inputStream)
        val actualContent = schemaFile.inputStream!!.reader().readText() // Convert InputStream to String
        assertEquals(mockContent, actualContent)
        assertEquals(fileName, schemaFile.fileName)
        verify(mockS3Client, times(1)).getSchemaFile(fileName)
    }

}




