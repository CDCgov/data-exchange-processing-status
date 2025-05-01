package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.QueryBuilder
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository


/**
 * Abstract class to reduce duplicate code to produce very common where clauses, etc.
 *
 * @constructor
 */
abstract class ReportQuery(
    repository: ProcessingStatusRepository,
    private val dataStreamIds: List<String>,
    private val dataStreamRoutes: List<String>,
    private val jurisdictions: List<String>
): QueryBuilder(repository.reportsCollection), ReportQuerySqlBuilder {

    /**
     * The [ReportQuery.Builder] can be extended to include additional properties.  Use the following a template for a
     * nested [Builder] class inside your query that needs additional properties in addition to what is in the
     * [ReportQuery] abstract base class.
     *
     * ```
     * class Builder(repository: ProcessingStatusRepository): ReportQuery.Builder<Builder>(repository) {
     *     private var myNewProperty: String = ""
     *
     *     fun withMyNewProperty(myNewProperty: String): Builder {
     *         this.myNewProperty = myNewProperty
     *         return this
     *     }
     *
     *     override fun build() = YourNewQuery(repository, dataStreamIds, dataStreamRoutes, jurisdictions, myNewProperty)
     * }
     * ```
     * @param T: Builder<T>
     * @property repository ProcessingStatusRepository
     * @property dataStreamIds List<String>
     * @property dataStreamRoutes List<String>
     * @property jurisdictions List<String>
     * @constructor
     */
    open class Builder<T: Builder<T>>(
        protected val repository: ProcessingStatusRepository
    ): ReportQueryBuilder {
        protected var dataStreamIds = listOf<String>()
        protected var dataStreamRoutes = listOf<String>()
        protected var jurisdictions = listOf<String>()

        @Suppress("UNCHECKED_CAST")
        fun withDataStreamIds(dataStreamIds: List<String>): T {
            this.dataStreamIds = dataStreamIds
            return this as T
        }

        @Suppress("UNCHECKED_CAST")
        fun withDataStreamRoutes(dataStreamRoutes: List<String>): T {
            this.dataStreamRoutes = dataStreamRoutes
            return this as T
        }

        @Suppress("UNCHECKED_CAST")
        fun withJurisdictions(jurisdictions: List<String>): T {
            this.jurisdictions = jurisdictions
            return this as T
        }

        override fun build(): ReportQuery {
            throw NotImplementedError("This function must be implemented by derived classes")
        }
    }

    /**
     * Constructs a where clause for a SQL query based on the provided class fields and their values.
     * The where clause will include conditions for `dataStreamIds`, `dataStreamRoutes`, and `jurisdictions`
     * if the respective collections are not empty.
     *
     * @param prefix A string to prefix the where clause. Defaults to "WHERE".
     * @return A SQL where clause as a string. Returns an empty string if no conditions are generated.
     */
    protected open fun whereClause(prefix: String = "WHERE"): String {
        val dataStreamIdsList = listForQuery(dataStreamIds)
        val dataStreamRoutesList = listForQuery(dataStreamRoutes)
        val jurisdictionIdsList = listForQuery(jurisdictions)

        val clauses = mutableListOf<String>()

        if (dataStreamIds.isNotEmpty()) {
            clauses.add("${cPrefix}dataStreamId IN ${openBkt}$dataStreamIdsList${closeBkt}")
        }

        if (dataStreamRoutesList.isNotEmpty()) {
            clauses.add("${cPrefix}dataStreamRoute IN ${openBkt}$dataStreamRoutesList${closeBkt}")
        }

        if (jurisdictionIdsList.isNotEmpty()) {
            clauses.add("${cPrefix}jurisdiction IN ${openBkt}$jurisdictionIdsList${closeBkt}")
        }

        if (clauses.isEmpty()) return ""

        return "$prefix ${clauses.joinToString(" AND ")} " // add space at the end in case other clauses follow
    }

    /**
     * Generic query runner that should be called by the specialized query classes.
     *
     * @param classType Class<T>?
     * @return List<T>
     */
    protected fun<T> runQuery(classType: Class<T>?): List<T> {
        return runCatching {
            val query = buildSql()
            logger.info("Executing query '${javaClass.simpleName}'")
            return@runCatching collection.queryItems(query, classType)
        }.getOrElse {
            logger.error("Error occurred while running query '${javaClass.simpleName}': ${it.localizedMessage}")
            throw it
        }
    }
}