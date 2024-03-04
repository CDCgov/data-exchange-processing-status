package gov.cdc.ocio.dispatcher

import gov.cdc.ocio.model.cache.NotificationSubscription
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailDispatcher(): IDispatcher {
    override fun dispatchEvent(subscription: NotificationSubscription): String {

// Call the function to check SMTP status
        return checkSMTPStatusWithoutCredentials(subscription)
    }

    private fun checkSMTPStatusWithoutCredentials(subscription: NotificationSubscription): String {
        // THis is to get the status from curl statement to see if server is connected
        try {
            // Replace placeholders with your SMTP server details
            val smtpServer = "smtpgw.cdc.gov"
            val port = 25 // Default SMTP port
            val socket = Socket(smtpServer, port)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            // Read the server response
            val response = reader.readLine()
            println("Server response: $response")
            // Close the socket
            socket.close()
            return response
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun checkSMTPStatusWithCredentials() {
        val username = "your_smtp_username"
        val password = "your_smtp_password"
        val smtpServer = "smtpgw.cdc.gov"
        val port = "25"
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = smtpServer
        props["mail.smtp.port"] = port
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })
        try {
            val transport = session.getTransport("smtp")
            transport.connect()
            transport.close()
            println("SMTP server is reachable.")
        } catch (e: MessagingException) {
            println("Error connecting to SMTP server: ${e.message}")
        }
    }

    private fun sendEmail() {
        // Replace with your SMTP server and account details
        val host = "smtpgw.cdc.gov"
        val port = "25"
        val username = "your_email@example.com"
        val password = "your_email_password"
        // Set up properties for the mail session
        val properties = Properties()
        properties["mail.smtp.host"] = host
        properties["mail.smtp.port"] = port
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        // Create a session with authentication
        val session = Session.getInstance(properties, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                return javax.mail.PasswordAuthentication(username, password)
            }
        })
        try {
            // Create a MimeMessage object
            val message = MimeMessage(session)
            // Set the sender's address
            message.setFrom(InternetAddress(username))
            // Set the recipient's address
            message.addRecipient(Message.RecipientType.TO, InternetAddress("donotreply@cdc.gov"))
            // Set the subject and content
            message.subject = "Test Email"
            message.setText("This is a test email sent from a Kotlin app using javax.mail.")
            // Send the message
            Transport.send(message)
            println("Email sent successfully.")
        } catch (e: MessagingException) {
            e.printStackTrace()
            println("Error sending email: ${e.message}")
        }
    }

}