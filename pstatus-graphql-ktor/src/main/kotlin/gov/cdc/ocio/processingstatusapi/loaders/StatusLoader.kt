package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.query.DuplicateFilenameCounts
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import gov.cdc.ocio.processingstatusapi.models.query.UploadStats
import gov.cdc.ocio.processingstatusapi.utils.DateUtils
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class StatusLoader: CosmosLoader() {

    @Throws(BadRequestException::class)
    fun getUploadStats(dataStreamId: String,
                       dataStreamRoute: String,
                       dateStart: String?,
                       dateEnd: String?,
                       daysInterval: String?): UploadStats {

        val timeRangeWhereClause = buildSqlClauseForDateRange(
            daysInterval,
            dateStart,
            dateEnd
        )

        val numUniqueUploadsQuery = (
                "select r.uploadId from $reportsContainerName r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "$timeRangeWhereClause group by r.uploadId"
                )

        val numUploadsWithStatusQuery = (
                "select value count(1) "
                        + "from $reportsContainerName r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.content.schema_name = 'upload' and "
                        + timeRangeWhereClause
                )

        val badMetadataCountQuery = (
                "select value count(1) "
                        + "from $reportsContainerName r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.content.schema_name = 'dex-metadata-verify' and "
                        + "ARRAY_LENGTH(r.content.issues) > 0 and $timeRangeWhereClause"
                )

        val inProgressUploadsCountQuery = (
                "select value count(1) "
                        + "from $reportsContainerName r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = 'dataStreamRoute' and "
                        + "r.content.schema_name = 'upload' and "
                        + "r.content['offset'] < r.content['size'] and $timeRangeWhereClause"
                )

        val completedUploadsCountQuery = (
                "select value count(1) "
                        + "from $reportsContainerName r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = '$dataStreamRoute' and "
                        + "r.content.schema_name = 'upload' and "
                        + "r.content['offset'] = r.content['size'] and $timeRangeWhereClause"
                )

        val duplicateFilenameCountQuery = (
                "select * from "
                        + "(select r.content.metadata.filename, count(1) as totalCount "
                        + "from $reportsContainerName r "
                        + "where r.dataStreamId = '$dataStreamId' and r.dataStreamRoute = 'dataStreamRoute' and "
                        + "r.content.schema_name = 'dex-metadata-verify' and "
                        + "$timeRangeWhereClause "
                        + "group by r.content.metadata.filename"
                        + ") r where r.totalCount > 1"
                )

        val uniqueUploadIdsResult = reportsContainer.queryItems(
            numUniqueUploadsQuery, CosmosQueryRequestOptions(),
            UploadCounts::class.java
        )

        val uniqueUploadIdsCount = uniqueUploadIdsResult.count()

        val uploadsWithStatusResult = reportsContainer.queryItems(
            numUploadsWithStatusQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val uploadsWithStatusCount = uploadsWithStatusResult.firstOrNull() ?: 0

        val badMetadataCountResult = reportsContainer.queryItems(
            badMetadataCountQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val badMetadataCount = badMetadataCountResult.firstOrNull() ?: 0

        val inProgressUploadCountResult = reportsContainer.queryItems(
            inProgressUploadsCountQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val inProgressUploadsCount = inProgressUploadCountResult.firstOrNull() ?: 0

        val completedUploadsCountResult = reportsContainer.queryItems(
            completedUploadsCountQuery, CosmosQueryRequestOptions(),
            Float::class.java
        )

        val completedUploadsCount = completedUploadsCountResult.firstOrNull() ?: 0

        val duplicateFilenameCountResult = reportsContainer.queryItems(
            duplicateFilenameCountQuery, CosmosQueryRequestOptions(),
            DuplicateFilenameCounts::class.java
        )

        val duplicateFilenames = if (duplicateFilenameCountResult.count() > 0)
            duplicateFilenameCountResult.toList() else listOf()

        return UploadStats().apply {
            this.uniqueUploadIdsCount = uniqueUploadIdsCount
            this.uploadsWithStatusCount = uploadsWithStatusCount.toInt()
            this.badMetadataCount = badMetadataCount.toInt()
            this.inProgressUploadsCount = inProgressUploadsCount.toInt()
            this.completedUploadsCount = completedUploadsCount.toInt()
            this.duplicateFilenames = duplicateFilenames
        }
    }

    @Throws(NumberFormatException::class, BadRequestException::class)
    private fun buildSqlClauseForDateRange(daysInterval: String?,
                                           dateStart: String?,
                                           dateEnd: String?): String {

        val timeRangeSqlPortion = StringBuilder()
        if (!daysInterval.isNullOrBlank()) {
            val dateStartEpochSecs = DateTime
                .now(DateTimeZone.UTC)
                .minusDays(Integer.parseInt(daysInterval))
                .withTimeAtStartOfDay()
                .toDate()
                .time / 1000
            timeRangeSqlPortion.append("r._ts >= $dateStartEpochSecs")
        } else {
            dateStart?.run {
                val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart, "date_start")
                timeRangeSqlPortion.append("r._ts >= $dateStartEpochSecs")
            }
            dateEnd?.run {
                val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd, "date_end")
                timeRangeSqlPortion.append(" and r._ts < $dateEndEpochSecs")
            }
        }
        return timeRangeSqlPortion.toString()
    }
}
