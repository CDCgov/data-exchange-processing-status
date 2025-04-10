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
abstract class CommonQuery(
    repository: ProcessingStatusRepository
): QueryBuilder(repository.reportsCollection) {

    /**
     * Use this to append the where class that filters by the data stream ids, data stream routes, jurisdictions,
     * and date provided.
     *
     * @param utcDateToRun LocalDate
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     * @return String
     */
    protected fun whereClause(
        utcDateToRun: LocalDate,
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>
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
}