package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.types.notification.Notifiable
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

data class UploadErrorSummary(
    val metadata: MetadataGroup,
    val failedMetadataVerifyCount: Int,
    val failedDeliveryCount: Int,
    val delayedUploads: List<String>,
    val delayedDeliveries: List<String>,
    val sinceDays: Int,
) : Notifiable {
    override fun subject(): String {
        return "PHDO UPLOAD ERROR SUMMARY NOTIFICATION"
    }

    override fun buildEmailBody(): String {
        return buildString {
            appendHTML().html {
                body {
                    h2 { +"${metadata.dataStreamRoute} ${metadata.dataStreamRoute} Upload Issues in the last $sinceDays days" }
                    br {  }
                    h3 { +"Total: ${failedMetadataVerifyCount + failedDeliveryCount + delayedUploads.size + delayedDeliveries.size }" }
                    ul {
                        li { +"Failed Metadata Validation: $failedMetadataVerifyCount" }
                        li { +"Failed Deliveries:  $failedDeliveryCount" }
                        li { +"Delayed Uploads: ${delayedUploads.size}" }
                        li { +"Delayed Deliveries: ${delayedDeliveries.size}" }
                    }
                    br {  }
                    h3 { +"Delayed Uploads" }
                    ul {
                        delayedUploads.map { li { +it } }
                    }
                    br {  }
                    h3 { +"Delayed Deliveries" }
                    ul {
                        delayedDeliveries.map{ li { +it }}
                    }
                }
            }
        }
    }

    override fun buildWebhookBody(): Any {
        TODO("Not yet implemented")
    }
}