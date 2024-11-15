package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.dao.*
import gov.cdc.ocio.processingstatusapi.models.query.DuplicateFilenameCounts
import gov.cdc.ocio.processingstatusapi.models.query.*
import gov.cdc.ocio.processingstatusapi.utils.SqlClauseBuilder
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


/**
 * Upload statistics loader.
 *
 * @property logger KLogger
 * @property repository ProcessingStatusRepository
 * @property reportsCollection Collection
 * @property cName String
 * @property cVar String
 * @property cPrefix String
 * @property openBkt Char
 * @property closeBkt Char
 * @property cElFunc Function1<String, String>
 * @property cArrayNotNullOrEmpty String
 */
class UploadStatsLoader: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val reportsCollection = repository.reportsCollection

    private val cName = reportsCollection.collectionNameForQuery
    private val cVar = reportsCollection.collectionVariable
    private val cPrefix = reportsCollection.collectionVariablePrefix
    private val openBkt = reportsCollection.openBracketChar
    private val closeBkt = reportsCollection.closeBracketChar
    private val cElFunc = reportsCollection.collectionElementForQuery
    private val cArrayNotNullOrEmpty = reportsCollection.isArrayNotEmptyOrNull

    @Throws(BadRequestException::class, ContentException::class)
    fun getUploadStats(
        dataStreamId: String,
        dataStreamRoute: String,
        dateStart: String?,
        dateEnd: String?,
        daysInterval: Int?
    ): UploadStats {

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(
            daysInterval,
            dateStart,
            dateEnd,
            cPrefix
        )

        val numUploadsWithStatusQuery = (
                "select uploadId "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-status' and "
                        + timeRangeWhereClause
                )

        val badMetadataCountQuery = (
                "select uploadId "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'metadata-verify' and "
                        + "$cArrayNotNullOrEmpty(${cPrefix}stageInfo.issues) > 0 and $timeRangeWhereClause"
                )

        val completedUploadsCountQuery = (
                "select uploadId "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-completed' and "
                        + "${cPrefix}stageInfo.status = 'SUCCESS' and "
                        + "$timeRangeWhereClause "
                )

        val uniqueUploadIdsCount = if (repository.supportsGroupBy) {
            val numUniqueUploadsQuery = (
                    "select ${cPrefix}uploadId from $cName $cVar "
                            + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                            + "$timeRangeWhereClause group by ${cPrefix}uploadId"
                    )
            val uniqueUploadIdsResult =
                reportsCollection.queryItems(
                    numUniqueUploadsQuery,
                    UploadCountsDao::class.java
                )
            uniqueUploadIdsResult.count()
        } else {
            // Less efficient than with group by, but same result
            val numUploadsQuery = (
                    "select * from $cName $cVar "
                            + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                            + timeRangeWhereClause
                    )
            val uploadIdsResult = reportsCollection.queryItems(
                numUploadsQuery,
                UploadCountsDao::class.java
            )
            uploadIdsResult.distinctBy { it.uploadId }.size
        }

        val uploadsWithStatusResult = reportsCollection.queryItems(
            numUploadsWithStatusQuery,
            UploadIdDao::class.java
        )

        val uploadsWithStatusCount = uploadsWithStatusResult.count()

        val badMetadataCountResult = reportsCollection.queryItems(
            badMetadataCountQuery,
            UploadIdDao::class.java
        )

        val badMetadataCount = badMetadataCountResult.count()

        val duplicateFilenames: List<DuplicateFilenameCounts>
        if (repository.supportsGroupBy) {
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
            val duplicateFilenameCountResult = reportsCollection.queryItems(
                duplicateFilenameCountQuery,
                DuplicateFilenameCountsDao::class.java
            )
            duplicateFilenames = duplicateFilenameCountResult.map { it.toDuplicateFilenameCounts() }
        } else {
            // Much less efficient way
            val duplicateFilenameCountQuery = (
                    "select ${cPrefix}content.metadata.received_filename "
                            + "from $cName $cVar "
                            + "where ${cPrefix}dataStreamId = '$dataStreamId' and ${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                            + "${cPrefix}stageInfo.service = 'UPLOAD API' and ${cPrefix}stageInfo.${cElFunc("action")} = 'metadata-verify' and "
                            + timeRangeWhereClause
                    )
            val duplicateFilenameCountResult = reportsCollection.queryItems(
                duplicateFilenameCountQuery,
                ReceivedFilenameDao::class.java
            )
            val result = duplicateFilenameCountResult
                .groupBy { it.receivedFilename } // group by filename
                .filter { it.value.size > 1 } // filter only duplicates (more than one of the same filename)
            duplicateFilenames = result.map {
                DuplicateFilenameCounts().apply {
                    this.filename = it.key
                    this.totalCount = it.value.size.toLong()
                }
            }
        }

        val completedUploadsCountResult = reportsCollection.queryItems(
            completedUploadsCountQuery,
            UploadIdDao::class.java
        )

        val completedUploadsCount = completedUploadsCountResult.count()

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
    @Throws(ContentException::class, BadRequestException::class)
    private fun getUndeliveredUploads(
        dataStreamId: String,
        dataStreamRoute: String,
        timeRangeWhereClause: String
    ): List<UndeliveredUpload> {
        try {
            // Determine all the uploadIds that can be categorized as undelivered
            // First, get all the uploads with an item for upload-complete or blob-file-copy
            val uploadsByState = getUploadsByState(dataStreamId, dataStreamRoute, timeRangeWhereClause)
            val uploadIdsWithIssues = identifyUndeliveredUploads(uploadsByState)

            val quotedIds = uploadIdsWithIssues.joinToString("\",\"", "\"", "\"")

            // Query to get the respective filenames for the above uploadIds with the select criteria
            val undeliveredUploadsQuery = buildUploadsQuery(dataStreamId, dataStreamRoute, quotedIds, timeRangeWhereClause)
            logger.info("UploadsStats, fetch all undelivered uploads query = $undeliveredUploadsQuery")

            val undeliveredUploads = executeQuery(
                undeliveredUploadsQuery,
                UndeliveredUploadDao::class.java
            ).map { it.toUndeliveredUpload() }

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

            val uploadsWithIssues = executeQuery(
                uploadsWithIssuesQuery,
                UndeliveredUploadDao::class.java
            ).map { it.toUndeliveredUpload() }

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
     * @param dataStreamId [String] - The ID of the data stream.
     * @param dataStreamRoute [String] - The route of the data stream.
     * @param expectedAction [String] - Expected "action" for a metadata-verify.
     * @param missingAction [String] - Missing "action" for a completed upload.
     * @param timeRangeWhereClause [String] - The SQL clause for the time range.
     * @return List<[String]> - A list of undelivered uploadIds.
     * @throws ContentException - If an error occurs while fetching results.
     * @throws BadRequestException - If an unknown error occurs while fetching results.
     */
    @Throws(ContentException::class, BadRequestException::class)
    private fun getUploadIdsWithIssues(
        dataStreamId: String,
        dataStreamRoute: String,
        expectedAction: String,
        missingAction: String,
        timeRangeWhereClause: String
    ): List<String> {

        try {
            // Query to retrieve the uploads with 'metadata-verify' action with the provided search criteria
            val expectedActionQuery = buildUploadByActionQuery(dataStreamId, dataStreamRoute, expectedAction, timeRangeWhereClause)

            // Query to retrieve the uploads with missing 'upload-completed' action with the provided search criteria
            val missingActionQuery = buildUploadByActionQuery(dataStreamId, dataStreamRoute, missingAction, timeRangeWhereClause)

            // Fetch the list of uploadIds
            val expectedActionIds = executeUploadsWithIssuesQuery(expectedActionQuery)
            val missingActionIds = executeUploadsWithIssuesQuery(missingActionQuery)

            return (expectedActionIds - missingActionIds).toList()

        } catch (e: ContentException) {
            logger.error("Error in getUploadIdsWithIssues: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error in getUploadIdsWithIssues: ${e.message}", e)
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
                UndeliveredUploadDao::class.java
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
                "select ${cPrefix}uploadId "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "${cPrefix}stageInfo.${cElFunc("action")} = '$action' and "
                        + timeRangeWhereClause
                )
    }

    /**
     * Retrieve the uploads with either an upload-completed or blob-file-copy report.
     *
     * @param dataStreamId The ID of the data stream.
     * @param dataStreamRoute The route of the data stream.
     * @param timeRangeWhereClause The SQL clause for the time range.
     * @return Matching uploads by state
     */
    private fun getUploadsByState(
        dataStreamId: String,
        dataStreamRoute: String,
        timeRangeWhereClause: String
    ): List<UploadDao> {

        val uploadsByStateQuery = (
                "select ${cPrefix}uploadId, "
                        + "${cPrefix}stageInfo.${cElFunc("action")}, "
                        + "${cPrefix}stageInfo.${cElFunc("status")} "
                        + "from $cName $cVar "
                        + "where "
                        + "${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}dataStreamRoute = '$dataStreamRoute' and "
                        + "( ${cPrefix}stageInfo.${cElFunc("action")} = 'upload-completed' or ${cPrefix}stageInfo.${cElFunc("action")} = 'blob-file-copy' ) and "
                        + timeRangeWhereClause
                )
        logger.info("UploadsStats, fetch uploadsByStateQuery query = $uploadsByStateQuery")
        return reportsCollection.queryItems(uploadsByStateQuery, UploadDao::class.java).toList()
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
        return (
                "select ${cPrefix}uploadId, ${cPrefix}content.filename "
                        + "from $cName $cVar "
                        + "where ${cPrefix}dataStreamId = '$dataStreamId' "
                        + "and ${cPrefix}dataStreamRoute = '$dataStreamRoute' "
                        + "and ${cPrefix}stageInfo.${cElFunc("action")} = 'metadata-verify' "
                        + "and ${cPrefix}uploadId IN ${openBkt}$quotedIds${closeBkt} "
                        + "and $timeRangeWhereClause"
                )
    }

    /**
     * Executes a query and returns the result.
     *
     * @param query The SQL query to execute.
     * @return A set of results.
     * @throws ContentException If an error occurs while executing the query.
     */
    @Throws(ContentException::class)
    private fun <T> executeQuery(query: String, classType: Class<T>?): List<T> {
        try {
            return reportsCollection.queryItems(query, classType).toList()
        } catch (e: ContentException) {
            logger.error("Error executing query, \"$query\": ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error executing query, \"$query\": ${e.message}", e)
            throw ContentException("Error executing query, \"$query\": ${e.message}")
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
    private fun identifyUndeliveredUploads(documents: List<UploadDao>): Set<String?> {
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