package gov.cdc.ocio.notificationdispatchers.email

/**
 * Enumerate all the supported email protocols.
 *
 * Note: There are other less common mail protocols such as Messaging Application Programming Interface (MAPI)
 * used by Outlook and Exchange.  If any others are supported in the future this enum will be expanded.
 *
 * @property value String
 * @constructor
 */
enum class EmailProtocol(val value: String) {
    SMTP("smtp")
}