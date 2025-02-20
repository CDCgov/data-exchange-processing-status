package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.security.SchemaSecurityConfig
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.koin.core.module.Module
import org.koin.dsl.module


object SchemaSecurityConfigKoinCreator {

    /**
     * The class which creates a schema security config based on the env vars.
     *
     * @param environment [ApplicationEnvironment]
     * @return [Module]
     */
    fun moduleFromAppEnv(environment: ApplicationEnvironment): Module {
        val schemaAdminSecretToken = environment.config.tryGetString("security.schema_admin_secret_token")
        return module {
            single { SchemaSecurityConfig(schemaAdminSecretToken) }
        }
    }
}