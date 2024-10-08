package gov.cdc.ocio.database.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken


/**
 * Base class for all processing status repositories
 *
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 */
abstract class ProcessingStatusRepository {

    // Common interface for the reports collection
    open lateinit var reportsCollection: Collection

    // Common interface for the reports deadletter collection
    open lateinit var reportsDeadLetterCollection: Collection

    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    /**
     * Interface and default implementation for doing report content transformations from the map to whatever type
     * the database is expecting.
     *
     * @param content Map<*, *>
     * @return Any?
     */
    open fun contentTransformer(content: Map<*, *>): Any? {
        val typeObject = object : TypeToken<HashMap<*, *>?>() {}.type
        val jsonMap: Map<String, Any> =
            gson.fromJson(Gson().toJson(content, MutableMap::class.java).toString(), typeObject)
        return jsonMap
    }
}