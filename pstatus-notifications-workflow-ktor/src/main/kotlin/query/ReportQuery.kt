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
     * Constructs a SQL WHERE clause based on the provided input lists for dataStreamIds, dataStreamRoutes,
     * and jurisdictions. Each non-empty list is converted into a comma-separated list enclosed in parentheses
     * and combined into a WHERE clause using SQL "AND" conditions.
     *
     * @return A string representation of the SQL WHERE clause with the chosen query conditions.
     */
    protected open fun whereClause(): String {
        val jurisdictionIdsList = listForQuery(jurisdictions)
        val dataStreamIdsList = listForQuery(dataStreamIds)
        val dataStreamRoutesList = listForQuery(dataStreamRoutes)

        val querySB = StringBuilder()

        if (dataStreamIds.isNotEmpty())
            querySB.append("""
                AND ${cPrefix}dataStreamId IN ${openBkt}$dataStreamIdsList${closeBkt}
                """)

        if (dataStreamRoutesList.isNotEmpty())
            querySB.append("""
                AND ${cPrefix}dataStreamRoute IN ${openBkt}$dataStreamRoutesList${closeBkt}
                """)

        if (jurisdictionIdsList.isNotEmpty())
            querySB.append("""
                AND ${cPrefix}jurisdiction IN ${openBkt}$jurisdictionIdsList${closeBkt}
                """)

        return querySB.toString().trimIndent()
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