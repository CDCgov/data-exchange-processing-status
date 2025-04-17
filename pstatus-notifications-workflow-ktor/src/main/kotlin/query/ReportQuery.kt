package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.QueryBuilder
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.types.InstantRange
import java.time.LocalDate


/**
 * Abstract class to reduce duplicate code to produce very common where clauses, etc.
 *
 * @constructor
 */
abstract class ReportQuery(
    private val name: String,
    repository: ProcessingStatusRepository,
    private val dataStreamIds: List<String>,
    private val dataStreamRoutes: List<String>,
    private val jurisdictions: List<String>
): QueryBuilder(repository.reportsCollection), ReportQuerySqlBuilder {

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
     * Use this to append the where class that filters by the data stream ids, data stream routes, jurisdictions,
     * and date provided.
     *
     * @param utcDateToRun LocalDate
     * @return String
     */
    protected fun whereClause(
        utcDateToRun: LocalDate
    ): String {
        val instantRange = InstantRange.fromLocalDate(utcDateToRun)
        val startTime = timeFunc(instantRange.start.epochSecond)
        val endTime = timeFunc(instantRange.endInclusive.epochSecond)

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

        querySB.append("""
                AND ${cPrefix}dexIngestDateTime >= $startTime
                AND ${cPrefix}dexIngestDateTime < $endTime
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
            logger.info("Executing query '$name'")
            return@runCatching collection.queryItems(query, classType)
        }.getOrElse {
            logger.error("Error occurred while running query '$name': ${it.localizedMessage}")
            throw it
        }
    }
}