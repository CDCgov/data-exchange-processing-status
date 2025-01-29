package gov.cdc.ocio.database.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Base class for all processing status repositories
 *
 * @property reportsCollection [Collection]
 * @property reportsDeadLetterCollection [Collection]
 * @property subscriptionManagementCollection [Collection]
 * @property notificationSubscriptionsCollection [Collection]
 */
abstract class ProcessingStatusRepository {

    val system = "Database"

    // Common interface for the reports collection
    open lateinit var reportsCollection: Collection

    // Common interface for the reports deadletter collection
    open lateinit var reportsDeadLetterCollection: Collection

    // Common interface for the subscription management collection
    open lateinit var subscriptionManagementCollection: Collection

    // Common interface for the notification subscriptions collection
    open lateinit var notificationSubscriptionsCollection: Collection

    abstract var healthCheckSystem: HealthCheckSystem

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