package gov.cdc.ocio.notificationdispatchers.email

import gov.cdc.ocio.notificationdispatchers.logger.LoggerDispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.DispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.NotificationContent
import gov.cdc.ocio.notificationdispatchers.model.SmtpConfig
import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging


/**
 * Dispatch worker for emails.
 *
 * @property environment ApplicationEnvironment
 * @property logger KLogger
 * @property emailDispatchWorker DispatchWorker
 * @constructor
 */
class EmailDispatchWorker(private val environment: ApplicationEnvironment) : DispatchWorker {
    private val logger = KotlinLogging.logger {}
    private val emailDispatchWorker = createDispatcher()

    /**
     * The email protocol will be determined from the [ApplicationEnvironment] and the corresponding email dispatcher
     * implementation created.
     *
     * @return DispatchWorker
     */
    private fun createDispatcher(): DispatchWorker {
        val protocol = environment.config.tryGetString("ktor.emailProtocol")?.lowercase().orEmpty()

        return if (protocol == EmailProtocol.SMTP.value) {
            logger.info { "Using SMTP email protocol" }

            val config = SmtpConfig(
                host = environment.config.tryGetString("smtp.host") ?: "smtpgw.cdc.gov",
                port = environment.config.tryGetString("smtp.port")?.toInt() ?: 25,
                auth = environment.config.tryGetString("smtp.auth")?.toBoolean() ?: false,
                username = environment.config.tryGetString("smtp.username"),
                password = environment.config.tryGetString("smtp.password"),
                enableTls = environment.config.tryGetString("smtp.auth")?.toBoolean() ?: true
            )

            SmtpEmailDispatchWorker(config)
        } else {
            logger.error { "Unsupported email protocol: $protocol. Falling back to logging notifications only." }
            LoggerDispatchWorker() // use the logger dispatcher instead
        }
    }

    override fun send(content: NotificationContent) = emailDispatchWorker.send(content)
}

