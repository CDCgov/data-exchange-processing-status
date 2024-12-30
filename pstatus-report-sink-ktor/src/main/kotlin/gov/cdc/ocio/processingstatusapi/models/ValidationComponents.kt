package gov.cdc.ocio.processingstatusapi.models

import com.fasterxml.jackson.databind.ObjectMapper

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy

import gov.cdc.ocio.database.utils.DateLongFormatTypeAdapter
import gov.cdc.ocio.database.utils.InstantTypeAdapter
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import gov.cdc.ocio.reportschemavalidator.errors.ErrorLoggerProcessor
import gov.cdc.ocio.reportschemavalidator.loaders.FileSchemaLoader
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.reportschemavalidator.validators.JsonSchemaValidator

import mu.KLogger
import mu.KotlinLogging

import java.time.Instant
import java.util.*

object ValidationComponents {
    private val objectMapper: ObjectMapper by lazy { ObjectMapper() }
    private val jsonUtils: DefaultJsonUtils by lazy { DefaultJsonUtils(objectMapper) }
    private val schemaLoader: FileSchemaLoader by lazy { FileSchemaLoader(mapOf(
        "REPORT_SCHEMA_LOCAL_FILE_SYSTEM_PATH" to "C:\\apps\\dex\\data-exchange-processing-status\\reports"

    )) }
    private val schemaValidator: JsonSchemaValidator by lazy { JsonSchemaValidator(SchemaValidation.logger) }
    private val errorProcessor: ErrorLoggerProcessor by lazy { ErrorLoggerProcessor(SchemaValidation.logger) }
    private val logger: KLogger by lazy { KotlinLogging.logger {} }

    private val gson: Gson by lazy {
        GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    data class Components(
        val objectMapper: ObjectMapper,
        val jsonUtils: DefaultJsonUtils,
        val schemaLoader: FileSchemaLoader,
        val schemaValidator: JsonSchemaValidator,
        val errorProcessor: ErrorLoggerProcessor,
        val logger: KLogger,
        val gson: Gson

    )

    fun getComponents(): Components {
        return Components(
            this.objectMapper,
            this.jsonUtils,
            this.schemaLoader,
            this.schemaValidator,
            this.errorProcessor,
            this.logger,
            this.gson
        )
    }

}