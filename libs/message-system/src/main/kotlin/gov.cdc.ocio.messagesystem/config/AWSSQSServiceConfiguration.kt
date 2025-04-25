package gov.cdc.ocio.messagesystem.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.smithy.kotlin.runtime.net.url.Url
import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import io.ktor.server.config.*
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import java.nio.file.Path


/**
 * The `AWSSQServiceConfiguration` class configures and initializes connection AWS SQS based on settings provided in an
 * `ApplicationConfig`. This class extracts necessary AWS credentials and configuration details, such as the SQS queue
 * URL, access key, secret key, and region, using the provided configuration path as a prefix.
 *
 * @param config `ApplicationConfig` containing the configuration settings for AWS SQS.
 * @param configurationPath represents prefix used to locate environment variables specific to AWS within the
 * configuration.
 */
class AWSSQSServiceConfiguration(
    config: ApplicationConfig,
    configurationPath: String? = null
) {
    lateinit var messageProcessor: MessageProcessorInterface

    private val configPath = if (configurationPath != null) "$configurationPath." else ""

    val listenQueueURL = config.tryGetString("${configPath}sqs.listenUrl") ?: ""
    val sendQueueURL = config.tryGetString("${configPath}sqs.sendUrl") ?: ""
    private val roleArn = config.tryGetString("${configPath}role_arn") ?: ""
    private val webIdentityTokenFile = config.tryGetString("${configPath}web_identity_token_file") ?: ""
    private val accessKeyId = config.tryGetString("${configPath}access_key_id") ?: ""
    private val secretAccessKey = config.tryGetString("${configPath}secret_access_key") ?: ""
    private val region = config.tryGetString("${configPath}region") ?: "us-east-1"
    private val endpoint = config.tryGetString("${configPath}endpoint")?.let { Url.parse(it) }

    fun createSQSClient(): SqsClient {
        return SqsClient {

            if (accessKeyId.isNotEmpty() && secretAccessKey.isNotEmpty()) {
                StaticCredentialsProvider {
                    accessKeyId = this@AWSSQSServiceConfiguration.accessKeyId
                    secretAccessKey = this@AWSSQSServiceConfiguration.secretAccessKey
                }
            } else if (webIdentityTokenFile.isNotEmpty() && roleArn.isNotEmpty()) {
                WebIdentityTokenFileCredentialsProvider.builder()
                    .roleArn(roleArn)
                    .webIdentityTokenFile(webIdentityTokenFile.let { Path.of(it) })
                    .build()
            } else {
                throw IllegalArgumentException("No valid credentials provided.")
            }
            region = this@AWSSQSServiceConfiguration.region
            endpointUrl = this@AWSSQSServiceConfiguration.endpoint
        }
    }
}
