package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.processingnotifications.email.EmailDispatcher

class NotificationActivitiesImpl : NotificationActivities {
    private val emailService: EmailDispatcher = EmailDispatcher()

    override fun sendNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        deliveryReference: String
    ) {
        val msg ="Sending the notification to $deliveryReference for dataStreamId: $dataStreamId, jurisdiction: $jurisdiction"
        println(msg)
        emailService.sendEmail("TEST EMAIL- Temporal Workflow execution update",msg, deliveryReference)
    }

    override fun sendUploadErrorsNotification(error: String, deliveryReference: String) {
        val msg ="Errors while upload. $error"
        println(msg)
        emailService.sendEmail("TEST EMAIL-UPLOAD ERRORS NOTIFICATION",msg, deliveryReference)
    }
}
