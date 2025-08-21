package gov.cdc.ocio.notificationdispatchers.model

/**
 * Model for SMTP configuration.
 *
 * @property host String
 * @property port Int
 * @property auth Boolean
 * @property username String?
 * @property password String?
 * @property enableTls Boolean
 * @constructor
 */
data class SmtpConfig(
    val host: String,
    val port: Int,
    val auth: Boolean = true,
    val username: String? = null,
    val password: String? = null,
    val enableTls: Boolean = true
)