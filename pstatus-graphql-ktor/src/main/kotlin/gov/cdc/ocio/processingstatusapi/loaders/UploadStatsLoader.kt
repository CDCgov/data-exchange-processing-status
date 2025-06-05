package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.database.utils.SqlClauseBuilder
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.query.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class UploadStatsLoader: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val reportsCollection = repository.reportsCollection

    private val cName = reportsCollection.collectionNameForQuery
    private val cVar = reportsCollection.collectionVariable
    private val cPrefix = reportsCollection.collectionVariablePrefix
    private val openBkt = reportsCollection.openBracketChar
    private val closeBkt = reportsCollection.closeBracketChar
    private val cElFunc = repository.reportsCollection.collectionElementForQuery
    private val timeFunc = repository.reportsCollection.timeConversionForQuery

    @Throws(BadRequestException::class, ContentException::class)
    fun getUploadStats(
        dataStreamId: String,
        dataStreamRoute: String,
        dateStart: String?,
        dateEnd: String?,
        daysInterval: Int?
    ): UploadStats {

        val timeRangeWhereClause = SqlClauseBuilder.buildSqlClauseForDateRange(
            daysInterval,
            dateStart,
            dateEnd,
            cPrefix,
            timeFunc
        )

        val numUniqueUploadsQuery = (
                "select ${cPrefix}uploadId from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "$timeRangeWhereClause group by ${cPrefix}uploadId"
                )

        val numUploadsWithStatusQuery = (
                "select value count(1) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-status' and "
                        + timeRangeWhereClause
                )

        val badMetadataCountQuery = (
                "select value count(1) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'metadata-verify' and "
                        + "ARRAY_LENGTH(${cPrefix}stageInfo.issues) > 0 and $timeRangeWhereClause"
                )

        val completedUploadsCountQuery = (
                "select value count(1) "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-completed' and "
                        + "${cPrefix}stageInfo.status = 'SUCCESS' and "
                        + "$timeRangeWhereClause "
                )

        val duplicateFilenameCountQuery = (
                "select * from "
                        + "(select ${cPrefix}content.metadata.received_filename, count(1) as totalCount "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'metadata-verify' and "
                        + "$timeRangeWhereClause "
                        + "group by ${cPrefix}content.metadata.received_filename"
                        + ") $cVar where ${cPrefix}totalCount > 1"
                )

        val uniqueUploadIdsResult = reportsCollection.queryItems(
            numUniqueUploadsQuery,
            UploadCounts::class.java
        )

        val uniqueUploadIdsCount = uniqueUploadIdsResult.count()

        val uploadsWithStatusResult = reportsCollection.queryItems(
            numUploadsWithStatusQuery,
            Float::class.java
        )

        val uploadsWithStatusCount = uploadsWithStatusResult.firstOrNull() ?: 0

        val badMetadataCountResult = reportsCollection.queryItems(
            badMetadataCountQuery,
            Float::class.java
        )

        val badMetadataCount = badMetadataCountResult.firstOrNull() ?: 0

        val duplicateFilenameCountResult = reportsCollection.queryItems(
            duplicateFilenameCountQuery,
            DuplicateFilenameCounts::class.java
        )

        val duplicateFilenames =
            if (duplicateFilenameCountResult.isNotEmpty())
                duplicateFilenameCountResult.toList()
            else
                listOf()

        val completedUploadsCountResult = reportsCollection.queryItems(
            completedUploadsCountQuery,
            Float::class.java
        )

        val completedUploadsCount = completedUploadsCountResult.firstOrNull() ?: 0

        val undeliveredUploads = getUndeliveredUploads(dataStreamId, dataStreamRoute, timeRangeWhereClause)
        val pendingUploads = getPendingUploads(dataStreamId, dataStreamRoute, timeRangeWhereClause)

        return UploadStats().apply {
            this.uniqueUploadIdsCount = uniqueUploadIdsCount.toLong()
            this.uploadsWithStatusCount = uploadsWithStatusCount.toLong()
            this.badMetadataCount = badMetadataCount.toLong()
            this.completedUploadsCount = completedUploadsCount.toLong()
            this.duplicateFilenames = duplicateFilenames
            this.undeliveredUploads.totalCount = undeliveredUploads.count().toLong()
            this.undeliveredUploads.undeliveredUploads = undeliveredUploads
            this.pendingUploads.totalCount = pendingUploads.count().toLong()
            this.pendingUploads.pendingUploads = pendingUploads
        }
    }

    /**
     * Retrieves uploads that require attention due to delivery issues within a specified time range.
     * The process involves:
     * 1. Fetching uploads with their completion and file copy states
     * 2. Identifying uploads with delivery issues
     * 3. Retrieving detailed information for the problematic uploads
     *
     * @param dataStreamId The identifier of the data stream to query
     * @param dataStreamRoute The route of the data stream to query
     * @param timeRangeWhereClause SQL clause specifying the time range filter
     * @return List of [UndeliveredUpload] containing details of uploads requiring attention
     *
     * @throws ContentException If there's an error accessing or processing the content
     * @throws BadRequestException If the request parameters are invalid or processing fails
     */
    @Throws(ContentException::class, BadRequestException :: class)
    private fun getUndeliveredUploads(
        dataStreamId: String,
        dataStreamRoute: String,
        timeRangeWhereClause: String
    ): List<UndeliveredUpload> {
        try {

            //All the uploadIds that can be categorized as undelivered
            //Query - Get all the uploads with an item for upload-complete or blob-file-copy
            val uploadsByStateQuery = buildUploadsByStateQuery(dataStreamId, dataStreamRoute, timeRangeWhereClause)
            logger.info("UploadsStats, fetch uploadsByStateQuery query = $uploadsByStateQuery")

            val uploadsByState = executeUploadsByStateQuery(uploadsByStateQuery)
            val uploadIdsWithIssues = identifyUndeliveredUploads(uploadsByState)

            val quotedIds = uploadIdsWithIssues.joinToString("\",\"", "\"", "\"")

            //Query to get the respective filenames for the above uploadIds with the select criteria
            val unDeliveredUploadsQuery = buildUploadsQuery(dataStreamId, dataStreamRoute, quotedIds, timeRangeWhereClause)
            logger.info("UploadsStats, fetch all undelivered uploads query = $unDeliveredUploadsQuery")

            val undeliveredUploads = executeUploadsQuery(unDeliveredUploadsQuery)
            return undeliveredUploads

        } catch (e: ContentException) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw e

        } catch (e: BadRequestException) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw BadRequestException("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}")
        }
    }


    /**
     * Retrieves uploads that have started the metadata verification process but haven't completed the upload process.
     * This function identifies and retrieves details for uploads that are potentially stalled in their processing pipeline.
     *
     * The process involves:
     * 1. Identifying uploads that have 'metadata-verify' status but no corresponding 'upload-completed' status
     * 2. For the identified uploads, fetching their detailed metadata and current state
     *
     * @param dataStreamId The identifier of the data stream to query
     * @param dataStreamRoute The route within the data stream to query
     * @param timeRangeWhereClause SQL clause specifying the time range filter
     * @return List<UndeliveredUpload> containing details of pending uploads. Returns empty list if no pending uploads are found
     *
     * @throws ContentException If there's an error accessing or processing the content store
     * @throws BadRequestException If the request parameters are invalid or processing fails
     */
    @Throws(ContentException::class, BadRequestException::class)
    private fun getPendingUploads(
        dataStreamId: String,
        dataStreamRoute: String,
        timeRangeWhereClause: String
    ): List<UndeliveredUpload> {
        try {
            // Get the uploadIds for each upload with 'metadata-verify' and no report for 'upload-completed'
            val pendingUploadIdsForBlobFileCopy = getUploadIdsWithIssues(
                dataStreamId,
                dataStreamRoute,
                "metadata-verify",
                "upload-completed",
                timeRangeWhereClause
            )

            if (pendingUploadIdsForBlobFileCopy.isEmpty()) {
                return emptyList()
            }

            // Then, fetch only the necessary metadata for these specific uploadIds
            val quotedIds = pendingUploadIdsForBlobFileCopy.joinToString("\",\"", "\"", "\"")

            val uploadsWithIssuesQuery = buildUploadsQuery(dataStreamId, dataStreamRoute, quotedIds, timeRangeWhereClause)
            logger.info("UploadsStats, uploadsWithIssuesQuery query = $uploadsWithIssuesQuery")

            val uploadsWithIssues = executeUploadsQuery(uploadsWithIssuesQuery)
            return uploadsWithIssues

        } catch (e: Exception) {
            logger.error("Error fetching uploads with issues: ${e.message}", e)
            throw when (e) {
                is ContentException, is BadRequestException -> e
                else -> BadRequestException("Error fetching uploads with issues: ${e.message}")
            }
        }
    }


    /**
     * Fetches the unmatched uploadIds for all the items without an associated item for blob-file-copy.
     *
     * @param dataStreamId The ID of the data stream.
     * @param dataStreamRoute The route of the data stream.
     * @param timeRangeWhereClause The SQL clause for the time range.
     * @return A list of undelivered uploadIds.
     * @throws ContentException, BadRequestException If an error occurs while fetching the undelivered upload IDs for blob-file-copy.
     */
    @Throws(ContentException::class, BadRequestException :: class, Exception:: class)
    private fun getUploadIdsWithIssues(
        dataStreamId: String,
        dataStreamRoute: String,
        expectedAction: String,
        missingAction: String,
        timeRangeWhereClause: String): List<String> {

        try{

            // Query to retrieve the uploads with 'metadata-verify' with the provided search criteria
            val expectedActionQuery = buildUploadByActionQuery(dataStreamId, dataStreamRoute, expectedAction, timeRangeWhereClause)

            // Query to retrieve the uploads with 'blob-file-copy' with the provided search criteria
            val missingActionQuery = buildUploadByActionQuery(dataStreamId, dataStreamRoute, missingAction, timeRangeWhereClause)

            // Fetch the list of undelivered uploadIds
            val expectedActionIds = executeUploadsWithIssuesQuery(expectedActionQuery)
            val missingActionIds = executeUploadsWithIssuesQuery(missingActionQuery)

            return (expectedActionIds - missingActionIds).toList()

        }catch (e: ContentException) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw e

        } catch (e: BadRequestException) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw e

        }catch (e: Exception) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw BadRequestException("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}")
        }

    }


    /**
     * Executes an undelivered upload query and returns the set of uploadIds.
     *
     * @param query The SQL query to execute.
     * @return A set of undelivered uploadIds.
     * @throws ContentException If an error occurs while executing the undelivered upload query.
     */
    @Throws(ContentException::class)
    private fun executeUploadsWithIssuesQuery(query: String): Set<String> {
        try {
            return reportsCollection.queryItems(
                query,
                UndeliveredUpload::class.java
            )
                .mapNotNull { it.uploadId }
                .toSet()
        } catch (e: ContentException) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw ContentException("Error executing undelivered uploads counts query: ${e.message}")
        }

    }

    /**
     * Executes a query against the reports collection to retrieve upload state information.
     * Converts the query results into a list of Upload objects representing upload states.
     *
     * @param query The SQL query string to execute against the reports collection
     * @return List<Upload> containing the query results. Returns empty list if no results found
     *
     * @throws ContentException If there's an error executing the query or processing the results
     */
    @Throws(ContentException::class)
    private fun executeUploadsByStateQuery(query: String): List<Upload> {
        try {
            return reportsCollection.queryItems(query, Upload::class.java).toList()
        } catch (e: ContentException) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw ContentException("Error executing undelivered uploads counts query: ${e.message}")
        }

    }

    /**
     * Builds a SQL query to retrieve the uploads by the specified data stream, route, and action.
     *
     * @param dataStreamId The ID of the data stream.
     * @param dataStreamRoute The route of the data stream.
     * @param action The action to filter by.
     * @param timeRangeWhereClause The SQL clause for the time range.
     * @return The SQL query string.
     */
    private fun buildUploadByActionQuery(
        dataStreamId: String,
        dataStreamRoute: String,
        action: String,
        timeRangeWhereClause: String
    ): String {
        return (
                "select distinct ${cPrefix}uploadId "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.${cElFunc("action")} = '$action' and "
                        + timeRangeWhereClause
                ).trimIndent()
    }

    /**
     * Builds a SQL query to retrieve the uploads with an item for blob-file-copy and a status of failure.
     *
     * @param dataStreamId The ID of the data stream.
     * @param dataStreamRoute The route of the data stream.
     * @param timeRangeWhereClause The SQL clause for the time range.
     * @return The SQL query string.
     */
    private fun buildUploadsByStateQuery(
        dataStreamId: String,
        dataStreamRoute: String,
        timeRangeWhereClause: String
    ): String {
        return (
                "select distinct ${cPrefix}uploadId, "
                        + "${cPrefix}stageInfo.${cElFunc("action")}, "
                        + "${cPrefix}stageInfo.${cElFunc("status")} "
                        + "from $cName $cVar "
                        + "where "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "( ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-completed' or ${cPrefix}stageInfo.${cElFunc("action")} = 'blob-file-copy' ) and "
                        + "$timeRangeWhereClause "
                )
    }

    /**
     *
     * @param dataStreamId [String] The ID of the data stream.
     * @param dataStreamRoute [String] The route of the data stream.
     * @param quotedIds [String]
     * @param timeRangeWhereClause [String] The SQL clause for the time range.
     * @return [String] The SQL query string.
     */
    private fun buildUploadsQuery(
        dataStreamId: String,
        dataStreamRoute: String,
        quotedIds: String,
        timeRangeWhereClause: String
    ): String {
        return """
            SELECT distinct ${cPrefix}uploadId, ${cPrefix}content.filename 
            FROM $cName $cVar 
            WHERE ${cPrefix}dataStreamId = '$dataStreamId' 
            AND ${cPrefix}dataStreamRoute = '$dataStreamRoute' 
            AND ${cPrefix}stageInfo.${cElFunc("action")} = 'metadata-verify' 
            AND ${cPrefix}uploadId IN ${openBkt}$quotedIds${closeBkt} 
            AND $timeRangeWhereClause
        """.trimIndent()
    }

    /**
     * Executes an undelivered upload query and returns the set of uploadIds.
     *
     * @param query The SQL query to execute.
     * @return A set of undelivered uploadIds.
     * @throws ContentException If an error occurs while executing the undelivered upload query.
     */
    @Throws(ContentException::class)
    private fun executeUploadsQuery(query: String): List<UndeliveredUpload> {
        try {
            return reportsCollection.queryItems(query, UndeliveredUpload::class.java).toList()
        } catch (e: ContentException) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw ContentException("Error executing undelivered uploads counts query: ${e.message}")
        }
    }

    /**
     * Identifies uploads with incomplete or failed file copy operations.
     * Analyzes upload documents to find cases where:
     * 1. Upload completed but file copy was never attempted
     * 2. One or more file copy operations failed
     *
     * @param documents List of Upload objects to analyze
     * @return Set of uploadIds requiring attention
     */
    private fun identifyUndeliveredUploads(documents: List<Upload>): Set<String?> {
        // Track status for each uploadId
        data class UploadStatus(
            var hasCompleted: Boolean = false,
            var hasCopy: Boolean = false,
            var hasFailedCopy: Boolean = false
        )

        val uploadStatuses = mutableMapOf<String?, UploadStatus>()

        // Single pass through documents
        documents.forEach { doc ->
            val status = uploadStatuses.getOrPut(doc.uploadId) { UploadStatus() }

            when (doc.action) {
                "upload-completed" -> {
                    status.hasCompleted = true
                }
                "blob-file-copy" -> {
                    status.hasCopy = true
                    if (doc.status != "SUCCESS") {
                        status.hasFailedCopy = true
                    }
                }
            }
        }

        // Return uploadIds that meet the criteria
        return uploadStatuses
            .filter { (_, status) ->
                status.hasCompleted && (
                        !status.hasCopy || // No blob-file-copy exists
                                status.hasFailedCopy // Has failed copy
                        )
            }
            .keys
    }
}