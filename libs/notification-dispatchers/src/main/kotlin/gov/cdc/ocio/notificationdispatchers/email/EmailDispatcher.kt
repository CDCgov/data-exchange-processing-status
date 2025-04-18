package gov.cdc.ocio.notificationdispatchers.email


fun interface EmailDispatcher {

    /**
     * Single function interface for sending an email.
     *
     * @param to List<String> list of email addresses
     * @param fromEmail String Example: "donotreply@cdc.gov"
     * @param fromName String Example: "Do not reply (PHDO team)"
     * @param subject String
     * @param body String
     */
    fun send(
        to: List<String>,
        fromEmail: String,
        fromName: String,
        subject: String,
        body: String
    )
}
