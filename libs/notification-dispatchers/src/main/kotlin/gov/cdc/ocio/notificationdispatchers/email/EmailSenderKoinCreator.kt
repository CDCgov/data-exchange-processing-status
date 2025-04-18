package gov.cdc.ocio.notificationdispatchers.email

import gov.cdc.ocio.notificationdispatchers.model.EmailDispatcherType
import gov.cdc.ocio.notificationdispatchers.model.SmtpConfig
import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module


object EmailSenderKoinCreator {

    /**
     * Creates a koin module and injects a singleton for the email sender from the [ApplicationEnvironment]
     *
     * @param environment ApplicationEnvironment
     * @return Module
     */
    fun moduleFromAppEnv(environment: ApplicationEnvironment): Module {
        val logger = KotlinLogging.logger {}

        val emailDispatcherModule = module {
            val emailDispatcher =  environment.config.tryGetString("ktor.emailDispatcher")
            when (emailDispatcher?.lowercase() ?: "") {
                EmailDispatcherType.SMTP.value -> {
                    // Default to the CDC SMTP gateway configuration for dispatching emails
                    val smtpConfig = SmtpConfig(
                        host = environment.config.tryGetString("smtp.host") ?: "smtpgw.cdc.gov",
                        port = environment.config.tryGetString("smtp.port")?.toInt() ?: 25,
                        auth = environment.config.tryGetString("smtp.auth")?.toBoolean() ?: false,
                        username = environment.config.tryGetString("smtp.username"),
                        password = environment.config.tryGetString("smtp.password"),
                        enableTls = environment.config.tryGetString("smtp.auth")?.toBoolean() ?: true
                    )
                    logger.info { "Using SMTP email dispatcher" }
                    single<EmailDispatcher> { SmtpEmailDispatcher(smtpConfig) }
                }

                EmailDispatcherType.LOGGER.value -> {
                    logger.info { "Using log-only email dispatcher (no emails actually sent)" }
                    single<EmailDispatcher> { LoggingEmailDispatcher() }
                }

                else -> {
                    logger.error("Unsupported email dispatcher requested: $emailDispatcher. Attempts to send emails will be logged only.")
                    // Use the logging one
                    single<EmailDispatcher> { LoggingEmailDispatcher() }
                }
            }
        }
        return emailDispatcherModule
    }
}