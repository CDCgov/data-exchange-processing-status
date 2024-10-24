package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.query.*
import gov.cdc.ocio.processingstatusapi.utils.SqlClauseBuilder
import java.util.*

class UploadStatsLoader: CosmosLoader() {

    @Throws(BadRequestException::class, ContentException::class)
    fun getUploadStats(dataStreamId: String,
                       dataStreamRoute: String,
                       dateStart: String?,
                       dateEnd: String?,
                       daysInterval: Int?
                       ): UploadStats {

        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(
            daysInterval,
            dateStart,
            dateEnd
        )

        val numUniqueUploadsQuery = (
                "select r.uploadId from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "$timeRangeWhereClause group by r.uploadId"
                )

        val numUploadsWithStatusQuery = (
                "select value count(1) "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.service = 'UPLOAD API' and r.stageInfo.action = 'upload-status' and "
                        + timeRangeWhereClause
                )

        val badMetadataCountQuery = (
                "select value count(1) "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.service = 'UPLOAD API' and r.stageInfo.action = 'metadata-verify' and "
                        + "ARRAY_LENGTH(r.stageInfo.issues) > 0 and $timeRangeWhereClause"
                )

        val completedUploadsCountQuery = (
                "select value count(1) "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.service = 'UPLOAD API' and r.stageInfo.action = 'upload-completed' and "
                        + "r.stageInfo.status = 'SUCCESS' and "
                        + "$timeRangeWhereClause "
                )

        val duplicateFilenameCountQuery = (
                "select * from "
                        + "(select r.content.metadata.received_filename, count(1) as totalCount "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.service = 'UPLOAD API' and r.stageInfo.action = 'metadata-verify' and "
                        + "$timeRangeWhereClause "
                        + "group by r.content.metadata.received_filename"
                        + ") r where r.totalCount > 1"
                )

        val uniqueUploadIdsResult = reportsContainer?.queryItems(
            numUniqueUploadsQuery, CosmosQueryRequestOptions(),
            UploadCounts::class.java
        )

        val uniqueUploadIdsCount = uniqueUploadIdsResult?.count() ?: 0

        val uploadsWithStatusResult = reportsContainer?.queryItems(
            numUploadsWithStatusQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val uploadsWithStatusCount = uploadsWithStatusResult?.firstOrNull() ?: 0

        val badMetadataCountResult = reportsContainer?.queryItems(
            badMetadataCountQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val badMetadataCount = badMetadataCountResult?.firstOrNull() ?: 0

        val duplicateFilenameCountResult = reportsContainer?.queryItems(
            duplicateFilenameCountQuery, CosmosQueryRequestOptions(),
            DuplicateFilenameCounts::class.java
        )

        val duplicateFilenames =
            if (duplicateFilenameCountResult != null && duplicateFilenameCountResult.count() > 0)
                duplicateFilenameCountResult.toList()
            else
                listOf()

        val completedUploadsCountResult = reportsContainer?.queryItems(
            completedUploadsCountQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val completedUploadsCount = completedUploadsCountResult?.firstOrNull() ?: 0

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
     * Searches the reports by uploadId to find undelivered uploads.
     *
     * @param dataStreamId The ID of the data stream.
     * @param dataStreamRoute The route of the data stream.
     * @param timeRangeWhereClause The SQL clause for the time range.
     * @return A list of [UnDeliveredUpload] objects representing the undelivered uploads.
     * @throws BadRequestException If an error occurs while fetching the undelivered uploads.
     */
    @Throws(ContentException::class, BadRequestException :: class)
    private fun getUndeliveredUploads(dataStreamId: String, dataStreamRoute: String, timeRangeWhereClause: String): List<UndeliveredUpload> {
        try{
            //Get the uploadIds for each upload with 'upload-completed' and no report for blob-file-copy
            val undeliveredUploadIdsForBlobFileCopy = getUploadIdsWithIssues(dataStreamId, dataStreamRoute, "upload-completed", "blob-file-copy", timeRangeWhereClause)

            //Query - Get all the uploads with an item for blob-file-copy and status of failure
            val unDeliveredUploadIdsQuery = buildBlobFileCopyFailureQuery(dataStreamId, dataStreamRoute, timeRangeWhereClause)
            logger.info("UploadsStats, fetch uploadIds query = $unDeliveredUploadIdsQuery")
            val uploadsWithFailures = executeUndeliveredUploadsQuery(unDeliveredUploadIdsQuery)

            //All the uploadIds that can be categorized as undelivered
            // (Any upload id where an upload-completed report exists but not a blob-file-copy report
            // or if there is the least one report with  blob-file-copy report and the status indicates failure.)
            val uploadIds = uploadsWithFailures + undeliveredUploadIdsForBlobFileCopy
            val quotedIds = uploadIds.joinToString("\",\"", "\"", "\"")

            //Query to get the respective filenames for the above uploadIds with the select criteria
            val unDeliveredUploadsQuery = buildUploadsQuery(dataStreamId, dataStreamRoute, quotedIds, timeRangeWhereClause)
            logger.info("UploadsStats, fetch all undelivered uploads query = $unDeliveredUploadsQuery")

            val undeliveredUploads = executeUploadsQuery(unDeliveredUploadsQuery)
            return undeliveredUploads

        }catch (e: ContentException) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw ContentException("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}")

        } catch (e: BadRequestException) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw BadRequestException("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}")
        }catch (e: Exception) {
            logger.error("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}", e)
            throw BadRequestException("Error fetching undelivered upload IDs for blob-file-copy: ${e.message}")
        }
    }


    /**
     * Searches the reports by uploadId to find undelivered uploads.
     *
     * @param dataStreamId The ID of the data stream.
     * @param dataStreamRoute The route of the data stream.
     * @param timeRangeWhereClause The SQL clause for the time range.
     * @return A list of [UnDeliveredUpload] objects representing the undelivered uploads.
     * @throws BadRequestException If an error occurs while fetching the undelivered uploads.
     */
    @Throws(ContentException::class, BadRequestException::class)
    private fun getPendingUploads(dataStreamId: String, dataStreamRoute: String, timeRangeWhereClause: String): List<UndeliveredUpload> {
        try{
            //Get the uploadIds for each upload with 'metadata-verify' and no report for 'upload-completed'
            val pendingUploadIdsForBlobFileCopy = getUploadIdsWithIssues(dataStreamId, dataStreamRoute, "metadata-verify", "upload-completed", timeRangeWhereClause)

            if (pendingUploadIdsForBlobFileCopy.isEmpty()) {
                return emptyList()
            }

            // Then, fetch only the necessary metadata for these specific uploadIds
            val quotedIds = pendingUploadIdsForBlobFileCopy.joinToString("\",\"", "\"", "\"")

            val uploadsWithIssuesQuery = buildUploadsQuery(dataStreamId, dataStreamRoute, quotedIds, timeRangeWhereClause)
            logger.info("UploadsStats, uploadsWithIssuesQuery query = $uploadsWithIssuesQuery")

            val uploadsWithIssues = executeUploadsQuery(uploadsWithIssuesQuery)
            return uploadsWithIssues

        }catch (e: Exception) {
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
            val expectedActionIds = executeUndeliveredUploadsQuery(expectedActionQuery)
            val missingActionIds = executeUndeliveredUploadsQuery(missingActionQuery)

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
    private fun executeUndeliveredUploadsQuery(query: String): Set<String> {
        try{

            val options = CosmosQueryRequestOptions().apply {
                maxDegreeOfParallelism = -1    // let SDK decide optimal number of concurrent operations
            }

            return reportsContainer?.queryItems(query, options, UndeliveredUpload::class.java)
                ?.mapNotNull { it.uploadId }
                ?.toSet() ?: emptySet()
        }catch (e: ContentException) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw e
        }catch (e: Exception) {
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
    private fun buildUploadByActionQuery(dataStreamId: String, dataStreamRoute: String, action: String, timeRangeWhereClause: String): String {
        return (
                "select distinct r.uploadId "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.action = '$action' and "
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
    private fun buildBlobFileCopyFailureQuery(dataStreamId: String, dataStreamRoute: String, timeRangeWhereClause: String): String {
        return (
                "select distinct r.uploadId "
                        + "from r "
                        + "where IS_DEFINED(r.content.content_schema_name) and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.content.content_schema_name = 'blob-file-copy' and "
                        + "r.stageInfo.action = 'blob-file-copy' and "
                        + "r.stageInfo.status = 'FAILURE' and "
                        + "$timeRangeWhereClause "
                )
    }


    private fun buildUploadsQuery(dataStreamId: String, dataStreamRoute: String, quotedIds: String, timeRangeWhereClause: String): String {
        return """
            SELECT distinct r.uploadId, r.content.filename 
            FROM r 
            WHERE r.dataStreamId = '$dataStreamId' 
            AND r.dataStreamRoute = '$dataStreamRoute' 
            AND r.stageInfo.action = 'metadata-verify' 
            AND r.uploadId IN ($quotedIds) 
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
        try{
            val options = CosmosQueryRequestOptions().apply {
                maxDegreeOfParallelism = -1    // let SDK decide optimal number of concurrent operations
            }
            return reportsContainer?.queryItems(query, options, UndeliveredUpload::class.java)
                ?.toList() ?: emptyList()
        }catch (e: ContentException) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw e
        }catch (e: Exception) {
            logger.error("Error executing undelivered uploads counts query: ${e.message}", e)
            throw ContentException("Error executing undelivered uploads counts query: ${e.message}")
        }
    }
}

