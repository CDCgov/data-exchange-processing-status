package gov.cdc.ocio.database.health.dbClientFactory

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.nio.file.Path

/**
 * Services call DynamoDbClientFactory.createDefaultClient() for a consistent client configuration.
 */
object DynamoDbClientFactory {
    fun createClient(region: String, roleArn: String?, webIdentityTokenFile: String?): DynamoDbClient {
      /*  val credentialsProvider = WebIdentityTokenFileCredentialsProvider.builder()
            .roleArn(roleArn)
            .webIdentityTokenFile(webIdentityTokenFile?.let { Path.of(it) })
            .build()*/

        val credentialsProvider =    if (roleArn.isNullOrEmpty() ||
            webIdentityTokenFile.isNullOrEmpty()) {
            // Fallback to default credentials provider (access key and secret)
            DefaultCredentialsProvider.create()
        } else {
            // Use Web Identity Token
            WebIdentityTokenFileCredentialsProvider.builder()
                .roleArn(roleArn)
                .webIdentityTokenFile(webIdentityTokenFile.let { Path.of(it) })
                .build()
        }

        return DynamoDbClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
    }
}
