package gov.cdc.ocio.notificationdispatchers.email

import gov.cdc.ocio.notificationdispatchers.logger.LoggerDispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.DispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.SmtpConfig
import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging


/**
 * Dispatch worker for emails.
 *
 * @constructor
 */
class EmailDispatchWorker private constructor(
    delegate: DispatchWorker
) : DispatchWorker by delegate {

    companion object {
        /**
         * Public wrapper for creating the email dispatch worker.
         *
         * @param environment ApplicationEnvironment
         * @return EmailDispatchWorker
         */
        fun create(environment: ApplicationEnvironment): EmailDispatchWorker {
            val delegate = createDispatcher(environment)
            return EmailDispatchWorker(delegate)
        }

        /**
         * Creates the email dispatch worker from the [ApplicationEnvironment].
         *
         * @param environment ApplicationEnvironment
         * @return DispatchWorker
         */
        private fun createDispatcher(environment: ApplicationEnvironment): DispatchWorker {
            val logger = KotlinLogging.logger {}

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
                LoggerDispatchWorker()
            }
        }
    }
}


