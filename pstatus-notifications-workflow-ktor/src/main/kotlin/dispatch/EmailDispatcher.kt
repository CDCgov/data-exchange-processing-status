package gov.cdc.ocio.processingnotifications.dispatch

import gov.cdc.ocio.processingnotifications.model.DeadlineCheck
import gov.cdc.ocio.processingnotifications.model.Email
import gov.cdc.ocio.processingnotifications.model.UploadDigest
import gov.cdc.ocio.processingnotifications.model.UploadErrorSummary
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import javax.mail.internet.MimeMessage
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress


/**
 * The class which dispatches the email using SMTP
 */
class EmailDispatcher(
    private val emailAddresses: List<String>
) : Dispatcher() {
    private val logger = KotlinLogging.logger {}
    private val session: Session

    init {
        val props = System.getProperties()
        // TODO move to environment variables
        props["mail.smtp.host"] = "smtpgw.cdc.gov"
        props["mail.smtp.port"] = 25
        session = Session.getInstance(props, null)
    }

    override fun dispatch(data: Any) {
        val email = when (data) {
            is UploadErrorSummary -> buildUploadErrorSummaryEmail(data)
            is DeadlineCheck -> buildUploadDeadlineEmail(data)
            else -> buildDefaultEmail(data)
        }
        sendEmail(email)
    }

    private fun sendEmail(email: Email) {
        try {
            if (!checkSMTPStatusWithoutCredentials()) return

            val toEmail = emailAddresses.joinToString(",")
            val msg = MimeMessage(session)
            val replyToEmail = "donotreply@cdc.gov"
            val replyToName = "DoNotReply (PHDO Team)"
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
            msg.addHeader("format", "flowed")
            msg.addHeader("Content-Transfer-Encoding", "8bit")

            //TODO - Change the from and replyTo address after the new licensed account is created
            // Get the email addresses from the property
            msg.setFrom(InternetAddress(replyToEmail, replyToName))
            msg.replyTo = InternetAddress.parse(replyToEmail, false)
            msg.setSubject(email.subject, "UTF-8")
            msg.setText(email.body, "UTF-8", "html")
            msg.sentDate = Date()
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false))
            Transport.send(msg)
        } catch (e: Exception) {
            logger.error("Unable to send email ${e.message}")
        }
    }

    // default to emailing the data as a raw string
    private fun buildDefaultEmail(
        data: Any
    ): Email {
        return Email("PHDO NOTIFICATION", data.toString())
    }

    private fun buildUploadErrorSummaryEmail(
        summary: UploadErrorSummary,
    ): Email {
        return Email("PHDO UPLOAD ERROR SUMMARY NOTIFICATION", buildUploadErrorSummaryEmailBody(summary))
    }

    private fun buildUploadDeadlineEmail(data: DeadlineCheck): Email {
        val body = "Upload deadline over.  Failed to get upload for dataStreamId: ${data.dataStreamId}, jurisdiction: ${data.jurisdiction}, at ${data.timestamp}"
        return Email(
            "PHDO UPLOAD DEADLINE CHECK EXPIRED for ${data.jurisdiction} on ${data.timestamp}",
                body
            )
    }

    private fun buildUploadDigestEmail(data: UploadDigest): Email {
        return Email("PHDO DAILY UPLOAD DIGEST COUNTS NOTIFICATION", buildUploadDigestEmailBody(data))
    }

    private fun buildUploadErrorSummaryEmailBody(summary: UploadErrorSummary): String {
        return buildString {
            appendHTML().html {
                body {
                    h2 { +"${summary.metadata.dataStreamRoute} ${summary.metadata.dataStreamRoute} Upload Issues in the last ${summary.sinceDays} days" }
                    br {  }
                    h3 { +"Total: ${summary.failedMetadataVerifyCount + summary.failedDeliveryCount + summary.delayedUploads.size + summary.delayedDeliveries.size }" }
                    ul {
                        li { +"Failed Metadata Validation: ${summary.failedMetadataVerifyCount}" }
                        li { +"Failed Deliveries: ${summary.failedDeliveryCount}" }
                        li { +"Delayed Uploads: ${summary.delayedUploads.size}" }
                        li { +"Delayed Deliveries: ${summary.delayedDeliveries.size}" }
                    }
                    br {  }
                    h3 { +"Delayed Uploads" }
                    ul {
                        summary.delayedUploads.map { li { +it } }
                    }
                    br {  }
                    h3 { +"Delayed Deliveries" }
                    ul {
                        summary.delayedDeliveries.map{ li { +it }}
                    }
                }
            }
        }
    }

    private fun buildUploadDigestEmailBody(data: UploadDigest): String {
        return buildString {
            appendHTML().html {
                body {
                    h2 { +"Daily Upload Digest for Data Streams" }
                    div { +"Date: ${data.timestamp} (12:00:00am through 12:59:59pm UTC)" }
                    br
                    h3 { +"Summary" }
                    table {
                        if (data.counts.isEmpty()) {
                            +"No uploads found."
                        } else {
                            thead {
                                tr {
                                    th { +"Data Stream ID" }
                                    th { +"Data Stream Route" }
                                    th { +"Jurisdictions" }
                                    th { +"Upload Counts" }
                                }
                            }
                            tbody {
                                data.counts.forEach { (dataStreamId, dataStreamRoutes) ->
                                    dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                        tr {
                                            td { +dataStreamId }
                                            td { +dataStreamRoute }
                                            td { +jurisdictions.size.toString() }
                                            td { +jurisdictions.values.sum().toString() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (data.counts.isNotEmpty()) {
                        br
                        h3 { +"Details" }
                        table {
                            thead {
                                tr {
                                    th { +"Data Stream ID" }
                                    th { +"Data Stream Route" }
                                    th { +"Jurisdiction" }
                                    th { +"Upload Counts" }
                                }
                            }
                            tbody {
                                data.counts.forEach { (dataStreamId, dataStreamRoutes) ->
                                    dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                        jurisdictions.forEach { (jurisdiction, count) ->
                                            tr {
                                                td { +dataStreamId }
                                                td { +dataStreamRoute }
                                                td { +jurisdiction }
                                                td { +count.toString() }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    p {
                        +("Subscriptions to this email are managed by the Public Health Data Observability (PHDO) "
                                + "Processing Status (PS) API.  Use the PS API GraphQL interface to unsubscribe."
                                )
                    }
                    p {
                        a(href = "https://cdcgov.github.io/data-exchange/") { +"Click here" }
                        +" for more information."
                    }
                }
            }
        }
    }

    /**
     * Method to send email which checks the SMTP status and then invokes sendEmail.
     *
     * @param subject String
     * @param body String
     * @param toEmailAddresses List<String>
     */
//    private fun sendBatch(
//        body: String,
//    ) {
//        try {
//            if (!checkSMTPStatusWithoutCredentials()) return
//            // TODO : Change this into properties
//            val toEmailAddressesStr = emailAddresses.joinToString(",")
//            val props = System.getProperties()
//            props["mail.smtp.host"] = "smtpgw.cdc.gov"
//            props["mail.smtp.port"] = 25
//            val session = Session.getInstance(props, null)
//            sendEmail(session, toEmailAddressesStr, body)
//        } catch(e: Exception) {
//            logger.error("Unable to send email ${e.message}")
//        }
//    }
//
//    /**
//     * Method to send email.
//     *
//     * @param session Session
//     * @param toEmail String
//     * @param subject String
//     * @param body String
//     */
//    private fun sendEmail(session: Session?, toEmail: String?, body: String?) {
//        try {
//            val msg = MimeMessage(session)
//            val replyToEmail = "donotreply@cdc.gov"
//            val replyToName = "DoNotReply (PHDO Team)"
//            //set message headers
//            msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
//            msg.addHeader("format", "flowed")
//            msg.addHeader("Content-Transfer-Encoding", "8bit")
//
//            //TODO - Change the from and replyTo address after the new licensed account is created
//            // Get the email addresses from the property
//            msg.setFrom(InternetAddress(replyToEmail, replyToName))
//            msg.replyTo = InternetAddress.parse(replyToEmail, false)
//            msg.setSubject(subject, "UTF-8")
//            msg.setText(body, "UTF-8", "html")
//            msg.sentDate = Date()
//            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false))
//            Transport.send(msg)
//        } catch (e: Exception) {
//            logger.error("Unable to send email ${e.message}")
//        }
//    }

    /**
     * Method to check the status of the SMTP server
     */
    private fun checkSMTPStatusWithoutCredentials(): Boolean {
        // This is to get the status from curl statement to see if server is connected
        try {
            val smtpServer = "smtpgw.cdc.gov"
            val port = 25
            val socket = Socket(smtpServer, port)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            // Read the server response
            val response = reader.readLine()
            println("Server response: $response")
            // Close the socket
            socket.close()
            return response !=null
        } catch (e: Exception) {
            logger.error("Unable to send email. Error is ${e.message} \n. Stack trace : ${e.printStackTrace()}")
        }
        return false
    }
}