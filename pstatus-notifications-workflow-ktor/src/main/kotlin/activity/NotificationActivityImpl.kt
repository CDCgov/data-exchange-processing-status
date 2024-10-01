package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.processingnotifications.email.EmailDispatcher
import mu.KotlinLogging

/**
 * Implementation class for sending email notifications for various notifications
 */
class NotificationActivitiesImpl : NotificationActivities {
    private val emailService: EmailDispatcher = EmailDispatcher()
    private val logger = KotlinLogging.logger {}

    /**
     * Send notification method which uses the email service to send email when an upload fails
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param deliveryReference String
     */
    override fun sendNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        deliveryReference: String
    ) {
        val msg ="Upload deadline over. Failed to get the upload for dataStreamId: $dataStreamId, jurisdiction: $jurisdiction.Sending the notification to $deliveryReference "
        logger.info(msg)
        emailService.sendEmail("TEST EMAIL- UPLOAD DEADLINE CHECK EXPIRED",msg, deliveryReference)
    }
    /**
     * Send notification method which uses the email service to send email when there are errors in the upload file
     * @param error String
     * @param deliveryReference String
     */

    override fun sendUploadErrorsNotification(error: String, deliveryReference: String) {
        val msg ="Errors while upload. $error"
        logger.info(msg)
        emailService.sendEmail("TEST EMAIL-UPLOAD ERRORS NOTIFICATION",msg, deliveryReference)
    }

    /**
     * Send notification method which uses the email service to send email with the digest counts of the top errors in an upload
     * @param error String
     * @param deliveryReference String
     */

    override fun sendDataStreamTopErrorsNotification(error: String, deliveryReference: String) {
        logger.info(error)
        emailService.sendEmail("TEST EMAIL-DATA STREAM TOP ERRORS NOTIFICATION",error, deliveryReference)
    }
}
