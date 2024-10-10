package gov.cdc.ocio.eventreadersink.sink

import gov.cdc.ocio.eventreadersink.camel.AwsRoutes
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import gov.cdc.ocio.eventreadersink.cloud.AwsManager
import gov.cdc.ocio.eventreadersink.model.AwsConfig

class AwsSink {

    fun sinkSQSTopicSubscriptionToS3(awsConfig: AwsConfig) {
        // Initialize the Camel context
        val camelContext: CamelContext = DefaultCamelContext()

        // Configure AWS Components
        configureAwsComponents(awsConfig, camelContext)

        // Add routes
        camelContext.addRoutes(AwsRoutes(awsConfig))

        // Start the Camel context
        camelContext.start()
        Runtime.getRuntime().addShutdownHook(Thread { camelContext.stop() })
    }

    private fun configureAwsComponents(awsConfig: AwsConfig, camelContext: CamelContext) {
        val sqsComponent = AwsManager().configureAwsSQSComponent(awsConfig.accessKeyId, awsConfig.secretAccessKey, awsConfig.sqsRegion)
        camelContext.addComponent("aws2-sqs", sqsComponent)

        val s3Component = AwsManager().configureAwsS3Component(awsConfig.accessKeyId, awsConfig.secretAccessKey, awsConfig.s3Region)
        camelContext.addComponent("aws2-s3", s3Component)
    }
}
