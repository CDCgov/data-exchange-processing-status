package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.model.UploadInfo
import gov.cdc.ocio.processingnotifications.utils.SqlClauseBuilder
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class ReportService: KoinComponent {
    private val repository by inject<ProcessingStatusRepository>()
    private val cName = repository.reportsCollection.collectionNameForQuery
    private val cVar = repository.reportsCollection.collectionVariable
    private val cPrefix = repository.reportsCollection.collectionVariablePrefix
    private val cElFunc = repository.reportsCollection.collectionElementForQuery
    private val logger = KotlinLogging.logger {}

    fun countFailedReports(dataStreamId: String, dataStreamRoute: String, action: String, daysInterval: Int?): Int {
        val query = "select value count(1) from $cName $cVar " +
                "where ${cPrefix}stageInfo.${cElFunc("status")} = 'FAILURE' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = '$action' " +
                "and dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute'"

        return repository.reportsCollection.queryItems(appendTimeRange(query, daysInterval), Int::class.java).firstOrNull() ?: 0
    }

    fun getDelayedUploads(dataStreamId: String, dataStreamRoute: String, daysInterval: Int?): List<String> {
        // first, get uploads that have upload-started reports older than 1 hour
        val oneHourAgo = Instant.now().minusSeconds(3600).toEpochMilli()
        val uploadsStartedQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-started' " +
                "and ${cPrefix}dexIngestDateTime < $oneHourAgo"
        val uploadsStarted = repository.reportsCollection.queryItems(appendTimeRange(uploadsStartedQuery, daysInterval), UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // then, get uploads that have upload-completed reports older than 1 hour
        val uploadsCompletedQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-completed' " +
                "and ${cPrefix}dexIngestDateTime < $oneHourAgo"
        val uploadsCompleted = repository.reportsCollection.queryItems(appendTimeRange(uploadsCompletedQuery, daysInterval), UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // then take the difference of those to get uploads that don't have upload-completed
        return (uploadsStarted - uploadsCompleted).toList()
    }

    fun getDelayedDeliveries(dataStreamId: String, dataStreamRoute: String): List<String> {
        // first, get completed uploads older than 1 hour
        val oneHourAgo = Instant.now().minusSeconds(3600).epochSecond
        val uploadsCompletedQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-completed' " +
                "and ${cPrefix}dexIngestDateTime < '$oneHourAgo'"
        val uploadsCompleted = repository.reportsCollection.queryItems(uploadsCompletedQuery, UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // then, get uploads that have been successfully delivered
        val uploadsDeliveredQuery = "select distinct ${cPrefix}uploadId from $cName $cVar " +
                "where dataStreamId = '$dataStreamId' " +
                "and dataStreamRoute = '$dataStreamRoute' " +
                "and ${cPrefix}stageInfo.${cElFunc("action")} = 'blob-file-copy' " +
                "and ${cPrefix}dexIngestDateTime < '$oneHourAgo'"
        val uploadsDelivered = repository.reportsCollection.queryItems(uploadsDeliveredQuery, UploadInfo::class.java)
            .map { it.uploadId }
            .toSet()

        // finally, take the difference to get uploads that haven't been delivered in over an hour
        return (uploadsCompleted - uploadsDelivered).toList()
    }

    private fun appendTimeRange(query: String, daysInterval: Int?): String {
        if (daysInterval != null) {
            return "$query and ${SqlClauseBuilder().buildSqlClauseForDateRange(daysInterval, null, null, cPrefix)}"
        }

        return query
    }
}