package gov.cdc.ocio.eventreadersink.cloud

import org.apache.camel.component.aws2.s3.AWS2S3Component
import org.apache.camel.component.aws2.s3.AWS2S3Configuration
import org.apache.camel.component.aws2.sqs.Sqs2Component
import org.apache.camel.component.aws2.sqs.Sqs2Configuration

/**
 * Class for creating Aws Components
 */
class AwsManager {

    fun configureAwsSQSComponent(awsAccessKeyId: String, awsSecretAccessKey: String, awsRegion: String): Sqs2Component {

        val sqsComponent = Sqs2Component()
        val sqsConfiguration = Sqs2Configuration()
        sqsConfiguration.accessKey = awsAccessKeyId
        sqsConfiguration.secretKey = awsSecretAccessKey
        sqsConfiguration.region = awsRegion
        sqsComponent.configuration = sqsConfiguration
        return sqsComponent
    }

    fun configureAwsS3Component(awsAccessKeyId: String, awsSecretAccessKey: String, awsRegion: String): AWS2S3Component {

        val s3Component = AWS2S3Component()
        val s3Configuration = AWS2S3Configuration()
        s3Configuration.accessKey = awsAccessKeyId
        s3Configuration.secretKey = awsSecretAccessKey
        s3Configuration.region = awsRegion
        s3Component.configuration = s3Configuration
        return s3Component
    }

}