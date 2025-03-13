package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.processingnotifications.email.EmailDispatcher
import gov.cdc.ocio.processingnotifications.model.CheckUploadResponse
import mu.KotlinLogging
import java.time.LocalDate


/**
 * Implementation class for sending email notifications for various notifications
 */
class NotificationActivitiesImpl : NotificationActivities {

    private val logger = KotlinLogging.logger {}

    private val emailService = EmailDispatcher()

    /**
     * Send notification method which uses the email service to send email when an upload fails
     * @param dataStreamId String
     * @param jurisdiction String
     * @param emailAddresses List<String>
     */
    override fun sendNotification(
        dataStreamId: String,
        jurisdiction: String,
        emailAddresses: List<String>
    ) {
        val msg = ("Upload deadline over. Failed to get the upload for dataStreamId: $dataStreamId, "
                + "jurisdiction: $jurisdiction on " + LocalDate.now() + ".")

        logger.info(msg)
        emailService.sendEmail(
            "UPLOAD DEADLINE CHECK EXPIRED for $jurisdiction on " + LocalDate.now(),
            msg,
            emailAddresses
        )
    }

    /**
     * Send notification method which uses the email service to send email when there are errors in the upload file.
     *
     * @param error String
     * @param emailAddresses List<String>
     */
    override fun sendUploadErrorsNotification(
        error: List<CheckUploadResponse>,
        emailAddresses: List<String>
    ) {
        val msg = "Number of uploads with errors while uploading: ${error.size}"
        logger.info(msg)
        emailService.sendEmail(
            "TEST EMAIL-UPLOAD ERRORS NOTIFICATION",
            msg,
            emailAddresses)
    }

    /**
     * Send notification method which uses the email service to send email with the digest counts of the top errors in
     * an upload.
     *
     * @param emailBody String
     * @param emailAddresses List<String>
     */
    override fun sendDataStreamTopErrorsNotification(
        emailBody: String,
        emailAddresses: List<String>
    ) {
        logger.info(emailBody)
        emailService.sendEmail(
            "TEST EMAIL-DATA STREAM TOP ERRORS NOTIFICATION",
            emailBody,
            emailAddresses)
    }

    /**
     * Sends an email with the daily upload digest counts.
     *
     * @param emailBody String
     * @param emailAddresses List<String>
     */
    override fun sendDigestEmail(
        emailBody: String,
        emailAddresses: List<String>
    ) {
        emailService.sendEmail(
            "DAILY UPLOAD DIGEST COUNTS NOTIFICATION",
            emailBody,
            emailAddresses)
    }
}
