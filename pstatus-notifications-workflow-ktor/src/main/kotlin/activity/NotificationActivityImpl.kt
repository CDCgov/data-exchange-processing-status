package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.processingnotifications.email.EmailDispatcher
import gov.cdc.ocio.processingnotifications.model.CheckUploadResponse
import mu.KotlinLogging
import java.time.LocalDate


/**
 * Implementation class for sending email notifications for various notifications
 */
class NotificationActivitiesImpl : NotificationActivities {
    private val emailService: EmailDispatcher = EmailDispatcher()
    private val logger = KotlinLogging.logger {}

    /**
     * Send notification method which uses the email service to send email when an upload fails
     * @param dataStreamId String
     * @param jurisdiction String
     * @param deliveryReference String
     */
    override fun sendNotification(
        dataStreamId: String,
        jurisdiction: String,
        deliveryReference: String
    ) {
        val msg ="Upload deadline over. Failed to get the upload for dataStreamId: $dataStreamId, jurisdiction: $jurisdiction on "+ LocalDate.now()+ "."
        logger.info(msg)
        emailService.sendEmail("UPLOAD DEADLINE CHECK EXPIRED for $jurisdiction on " +LocalDate.now() + "",msg, deliveryReference)
    }

    /**
     * Send notification method which uses the email service to send email when there are errors in the upload file.
     *
     * @param error String
     * @param deliveryReference String
     */
    override fun sendUploadErrorsNotification(error: List<CheckUploadResponse>, deliveryReference: String) {
        val msg = "Number of uploads with errors while uploading: ${error.size}"
        logger.info(msg)
        emailService.sendEmail("TEST EMAIL-UPLOAD ERRORS NOTIFICATION", msg, deliveryReference)
    }

    /**
     * Send notification method which uses the email service to send email with the digest counts of the top errors in
     * an upload.
     *
     * @param error String
     * @param deliveryReference String
     */
    override fun sendDataStreamTopErrorsNotification(error: String, deliveryReference: String) {
        logger.info(error)
        emailService.sendEmail("TEST EMAIL-DATA STREAM TOP ERRORS NOTIFICATION",error, deliveryReference)
    }

    override fun sendDigestEmail(emailBody: String, deliveryReference: String) {
        emailService.sendEmail("DAILY UPLOAD DIGEST COUNTS NOTIFICATION",emailBody, deliveryReference)
    }
}
