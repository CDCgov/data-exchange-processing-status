package gov.cdc.ocio.reportschemavalidator.utils

import io.ktor.server.application.*
import org.koin.core.module.Module
import org.koin.dsl.module

object SchemaLoaderKoinCreator {

    /**
     * The class which creates a schema loader based on the env vars.
     *
     * @param environment [ApplicationEnvironment]
     * @return [Module]
     */
    fun moduleFromAppEnv(environment: ApplicationEnvironment): Module {
        return module {
            single { SchemaLoaderConfiguration.createSchemaLoader(environment) }
        }
    }
}