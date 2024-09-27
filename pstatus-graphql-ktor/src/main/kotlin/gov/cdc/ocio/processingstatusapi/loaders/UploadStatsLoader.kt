package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDeadLetterDao
import gov.cdc.ocio.processingstatusapi.models.query.*
import gov.cdc.ocio.processingstatusapi.utils.SqlClauseBuilder
import java.util.*

class UploadStatsLoader: CosmosLoader() {

    @Throws(BadRequestException::class)
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

        val inProgressUploadsCountQuery = (
                "select value count(1) "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.service = 'UPLOAD API' and r.stageInfo.action = 'upload-status' and "
                        + "r.content['offset'] < r.content['size'] and $timeRangeWhereClause"
                )

        val completedUploadsCountQuery = (
                "select value count(1) "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.service = 'UPLOAD API' and r.stageInfo.action = 'upload-status' and "
                        + "r.content['offset'] = r.content['size'] and $timeRangeWhereClause"
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

        val inProgressUploadCountResult = reportsContainer?.queryItems(
            inProgressUploadsCountQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val inProgressUploadsCount = inProgressUploadCountResult?.firstOrNull() ?: 0

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

        return UploadStats().apply {
            this.uniqueUploadIdsCount = uniqueUploadIdsCount.toLong()
            this.uploadsWithStatusCount = uploadsWithStatusCount.toLong()
            this.badMetadataCount = badMetadataCount.toLong()
            this.inProgressUploadsCount = inProgressUploadsCount.toLong()
            this.completedUploadsCount = completedUploadsCount.toLong()
            this.duplicateFilenames = duplicateFilenames
            this.unDeliveredUploads.totalCount = undeliveredUploads.count().toLong()
            this.unDeliveredUploads.unDeliveredUploads = undeliveredUploads
        }
    }

    /**
     * Search the reports by uploadId for unDelivered Uploads.
     *
     * @param dataStreamId
     * @param dataStreamRoute
     * @param timeRangeWhereClause
     * @return List<UnDeliveredUpload>
     */
    private fun getUndeliveredUploads(dataStreamId: String, dataStreamRoute: String, timeRangeWhereClause: String): List<UnDeliveredUpload> {

        //Query to get the upload Ids for the given criteria
        val unDeliveredUploadIdsQuery = (
                "select r.uploadId "
                        + "from r "
                        + "where IS_DEFINED(r.content.content_schema_name) and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.content.content_schema_name = 'blob-file-copy' and "
                        + "r.stageInfo.status = 'FAILURE' and "
                        + "$timeRangeWhereClause "
                )
        logger.info("UploadsStats, fetch uploadIds query = $unDeliveredUploadIdsQuery")

        val unDeliveredUploadIdsResult = reportsContainer?.queryItems(
            unDeliveredUploadIdsQuery, CosmosQueryRequestOptions(),
            UnDeliveredUpload::class.java
        )

        // Extract uploadIds from the result
        val uploadIds = unDeliveredUploadIdsResult?.mapNotNull { it.uploadId } ?: emptyList()
        val quotedIds = uploadIds.joinToString("\",\"", "\"", "\"")

        //Query to get the respective filenames for the above uploadIds with the select criteria
        val unDeliveredUploadsQuery = (
                "select r.uploadId, r.content.filename "
                        + "from r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.stageInfo.action = 'metadata-verify' and "
                        + "r.uploadId in ($quotedIds) and "
                        + "$timeRangeWhereClause "
                )
        logger.info("UploadsStats, fetch undelivered uploads query = $unDeliveredUploadsQuery")

        val unDeliveredUploadsResult = reportsContainer?.queryItems(
            unDeliveredUploadsQuery, CosmosQueryRequestOptions(),
            UnDeliveredUpload::class.java
        )

        val undeliveredUploads =
            if (unDeliveredUploadsResult != null && unDeliveredUploadsResult.count() > 0)
                unDeliveredUploadsResult.toList()
            else
                listOf()

        return undeliveredUploads

    }
}
