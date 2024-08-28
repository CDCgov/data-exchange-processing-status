package gov.cdc.ocio.processingstatusapi.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import mu.KotlinLogging
import java.io.File

class SchemaValidation {
    companion object {
        //Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
        val gson: Gson = GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create()
        val logger = KotlinLogging.logger {}

        lateinit var reason: String
    }
    /**
     * Loads a base schema file from resources folder and returns file object.
     * @param schemaDirectoryPath The directory path where schema file is located.
     * @param fileName The name of the schema file to load.
     */
    fun loadSchemaFile(schemaDirectoryPath: String, fileName: String): File?{
        val schemaFilePath = javaClass.classLoader.getResource("$schemaDirectoryPath/$fileName")
        return schemaFilePath?.let { File(it.toURI()) }?.takeIf { it.exists() }

    }
}