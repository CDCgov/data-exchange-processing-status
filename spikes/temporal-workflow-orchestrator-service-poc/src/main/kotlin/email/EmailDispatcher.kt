package gov.cdc.ocio.processingnotifications.email

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import javax.mail.internet.MimeMessage
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress

class EmailDispatcher {

    /*fun sendEmailUsingSendGrid(
        apiKey: String,
        toEmail: String,
        fromEmail: String,
        subject: String,
        body: String
    ) {
        val from = Email(fromEmail)
        val to = Email(toEmail)
        val content = Content("text/plain", body)
        val mail = Mail(from, subject, to, content)

        val sendGrid = SendGrid(apiKey)
        val request = Request()

        try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response = sendGrid.api(request)
            println("Response status: ${response.statusCode}")
            println("Response body: ${response.body}")
            println("Response headers: ${response.headers}")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }*/

    /**
     * Method to send email

     */
     fun sendEmail(subject:String,body:String, toEmail:String): Unit {

        try{

            if(!checkSMTPStatusWithoutCredentials()) return
            // TODO : Change this into properties
            val toEmalId = toEmail
            val props = System.getProperties()
            props["mail.smtp.host"] = "smtpgw.cdc.gov"
            props["mail.smtp.port"] = 25
            val session = Session.getInstance(props, null)
            sendEmail(session, toEmalId, subject,body)
        } catch(_: Exception) {
         //   logger.info("Unable to send email")
        }

    }
    fun sendEmail(session: Session?, toEmail: String?, subject: String?, body: String?) {
        try {
            val msg = MimeMessage(session)
            // TODO: Uncomment this later
//            val replyToEmail = System.getenv("ReplyToEmail")
//            val replyToName = System.getenv("ReplyToName")

            val replyToEmail = "donotreply@cdc.gov"
            val replyToName = "DoNOtReply (DEX Team)"
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
            msg.addHeader("format", "flowed")
            msg.addHeader("Content-Transfer-Encoding", "8bit")

            //TODO - Change the from and replyTo address after the new licensed account is created
            // Get the email addresses from the property
            msg.setFrom(InternetAddress(replyToEmail, replyToName))
            msg.replyTo = InternetAddress.parse(replyToEmail, false)
            msg.setSubject(subject, "UTF-8")
            msg.setText(body, "UTF-8")
            msg.sentDate = Date()
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false))
            Transport.send(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Method to check teh status of the SMTP server

     */
    private fun checkSMTPStatusWithoutCredentials(): Boolean {
        // This is to get the status from curl statement to see if server is connected
        try {
            // TODO : Uncomment this later
//            val smtpServer = System.getenv("SmtpHostServer")
//            val port = System.getenv("SmtpHostPort").toInt()

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
            e.printStackTrace()
        }
        return false
    }
}