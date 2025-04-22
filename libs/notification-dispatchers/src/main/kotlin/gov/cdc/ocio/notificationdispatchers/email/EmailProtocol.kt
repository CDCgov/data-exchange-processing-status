package gov.cdc.ocio.notificationdispatchers.email

/**
 * Enumerate all the supported email protocols.
 *
 * Note: There are a few other mail protocols less common email protocols like Messaging Application Programming
 * Interface (MAPI) for sending emails used by Outlook and Exchange.  If any others are supported in the future
 * this enum will be expanded.
 *
 * @property value String
 * @constructor
 */
enum class EmailProtocol(val value: String) {
    SMTP("smtp")
}