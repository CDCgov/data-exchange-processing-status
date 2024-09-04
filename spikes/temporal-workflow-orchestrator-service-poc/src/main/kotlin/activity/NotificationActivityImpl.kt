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
        val msg ="Upload deadline over. Failed to get the upload for dataStreamId: $dataStreamId, jurisdiction: $jurisdiction.Sending the notification to $deliveryReference "
        println(msg)
        emailService.sendEmail("TEST EMAIL- UPLOAD DEADLINE CHECK EXPIRED",msg, deliveryReference)
    }

    override fun sendUploadErrorsNotification(error: String, deliveryReference: String) {
        val msg ="Errors while upload. $error"
        println(msg)
        emailService.sendEmail("TEST EMAIL-UPLOAD ERRORS NOTIFICATION",msg, deliveryReference)
    }

    override fun sendDataStreamTopErrorsNotification(error: String, deliveryReference: String) {
        println(error)
        emailService.sendEmail("TEST EMAIL-DATA STREAM TOP ERRORS NOTIFICATION",error, deliveryReference)
    }
}
