package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
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

        val unDeliveredUploadsQuery = (
                "select r.uploadId, r.content.metadata.received_filename "
                        + "from r "
                        + "where IS_DEFINED(r.content.content_schema_name) and "
                        + "r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.content.content_schema_name = 'blob-file-copy' and "
                        + "r.stageInfo.status = 'FAILURE' and "
                        + "$timeRangeWhereClause "
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

        val unDeliveredUploadsCountResult = reportsContainer?.queryItems(
            unDeliveredUploadsQuery, CosmosQueryRequestOptions(),
            UnDeliveredUpload::class.java
        )

        val undeliveredUploads =
            if (unDeliveredUploadsCountResult != null && unDeliveredUploadsCountResult.count() > 0)
                unDeliveredUploadsCountResult.toList()
            else
                listOf()

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
}
