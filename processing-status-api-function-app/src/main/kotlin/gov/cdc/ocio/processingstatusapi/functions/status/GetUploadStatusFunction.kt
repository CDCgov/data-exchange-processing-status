package gov.cdc.ocio.processingstatusapi.functions.status

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.UploadStatus
import gov.cdc.ocio.processingstatusapi.model.UploadsStatus
import gov.cdc.ocio.processingstatusapi.model.reports.UploadCounts
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import mu.KotlinLogging
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Collection of ways to get upload status.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property context ExecutionContext
 * @constructor
 */
class GetUploadStatusFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {

    private val logger = KotlinLogging.logger {}

    private val gson = JsonUtils.getGsonBuilderWithUTC()

    private val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)!!
    }

    /**
     * Advanced query for dex uploads, including sorting, filtering and pagination.
     *
     * @param destinationId String
     * @param stageName String
     * @return HttpResponseMessage
     */
    fun uploadStatus(destinationId: String, stageName: String): HttpResponseMessage {

        logger.info("destinationId = $destinationId")

        val dateStart = request.queryParameters["date_start"]
        val dateEnd = request.queryParameters["date_end"]
        val pageSize = request.queryParameters["page_size"]
        val pageNumber = request.queryParameters["page_number"]
        val extEvent = request.queryParameters["ext_event"]
        val sortBy = request.queryParameters["sort_by"]
        val sortOrder = request.queryParameters["sort_order"]

        val pageSizeAsInt = try {
            getPageSize(pageSize)
        } catch (ex: BadRequestException) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(ex.localizedMessage)
                    .build()
        }

        val sqlQuery = StringBuilder()
        sqlQuery.append("from $reportsContainerName t where t.destinationId = '$destinationId'")

        extEvent?.run {
            sqlQuery.append(" and t.eventType = '$extEvent'")
        }

        dateStart?.run {
            try {
                val dateStartEpochSecs = getEpochFromDateString(dateStart, "date_start")
                sqlQuery.append(" and t._ts >= $dateStartEpochSecs")
            } catch (e: BadRequestException) {
                logger.error(e.localizedMessage)
                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(e.localizedMessage)
                        .build()
            }
        }
        dateEnd?.run {
            try {
                val dateEndEpochSecs = getEpochFromDateString(dateEnd, "date_end")
                sqlQuery.append(" and t._ts < $dateEndEpochSecs")
            } catch (e: BadRequestException) {
                logger.error(e.localizedMessage)
                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(e.localizedMessage)
                        .build()
            }
        }
        sqlQuery.append(" group by t.uploadId")

        // SELECT count(1) as reportCounts, t.uploadId from Reports t where t.destinationId = 'dex-testing' group by t.uploadId
        val countQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery"
        logger.info("upload status count query = $countQuery")

        var totalItems = 0L
        try {
            val count = reportsContainer.queryItems(
                countQuery, CosmosQueryRequestOptions(),
                UploadCounts::class.java
            )
            totalItems = count.count().toLong()
            logger.info("Upload status matched count = $totalItems")
        } catch (ex: Exception) {
            // no items found or problem with query
            logger.error(ex.localizedMessage)
        }

        val numberOfPages: Int
        val pageNumberAsInt: Int
        val reports = mutableMapOf<String, List<Report>>()
        if (totalItems > 0L) {
            numberOfPages =  (totalItems / pageSizeAsInt + if (totalItems % pageSizeAsInt > 0) 1 else 0).toInt()

            pageNumberAsInt = try {
                getPageNumber(pageNumber, numberOfPages)
            } catch (ex: BadRequestException) {
                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(ex.localizedMessage)
                        .build()
            }

            // order by not working
//            sortBy?.run {
//                val sortField = when (sortBy) {
//                    "date" -> "_ts"
//                    else -> {
//                        return request
//                                .createResponseBuilder(HttpStatus.BAD_REQUEST)
//                                .body("sort_by must be one of the following: [date]")
//                                .build()
//                    }
//                }
//                var sortOrderVal = DEFAULT_SORT_ORDER
//                sortOrder?.run {
//                    sortOrderVal = when (sortOrder) {
//                        "ascending" -> "asc"
//                        "descending" -> "desc"
//                        else -> {
//                            return request
//                                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
//                                    .body("sort_order must be one of the following: [ascending, descending]")
//                                    .build()
//                        }
//                    }
//                }
//                sqlQuery.append(" order by t.$sortField $sortOrderVal")
//            }
            // SELECT count(1) as reportCounts, t.uploadId, t._ts from Reports t where t.destinationId = 'dex-testing' group by t.uploadId, t._ts order by t._ts asc offset 0 limit 10
            val offset = (pageNumberAsInt - 1) * pageSizeAsInt
            val dataSqlQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery offset $offset limit $pageSizeAsInt"
            logger.info("upload status data query = $dataSqlQuery")
            val results = reportsContainer.queryItems(
                    dataSqlQuery, CosmosQueryRequestOptions(),
                    UploadCounts::class.java
            ).toList()

            results.forEach { report ->
                val uploadId = report.uploadId
                    ?: return request
                        .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Upload ID unexpectedly null")
                        .build()
                val reportsSqlQuery = "select * from $reportsContainerName t where t.uploadId = '$uploadId'"
                logger.info("get reports for upload query = $reportsSqlQuery")
                val reportsForUploadId = reportsContainer.queryItems(
                    reportsSqlQuery, CosmosQueryRequestOptions(),
                    Report::class.java
                ).toList()

                reports[uploadId] = reportsForUploadId
            }
        } else {
            numberOfPages = 0
            pageNumberAsInt = 0
        }

        val uploadsStatus = UploadsStatus()
        reports.forEach { report ->
            try {
               val uploadStatus = UploadStatus.createFromReports(uploadId = report.key, reports = report.value)
                uploadsStatus.items.add(uploadStatus)
            } catch (e: ContentException) {
                logger.error("Unable to convert stage report with name, \"$stageName\" to upload status: ${e.localizedMessage}")
            }
        }

        uploadsStatus.summary.pageNumber = pageNumberAsInt
        uploadsStatus.summary.pageSize = pageSizeAsInt
        uploadsStatus.summary.numberOfPages = numberOfPages
        uploadsStatus.summary.totalItems = totalItems

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(gson.toJson(uploadsStatus))
                .build()
    }

    /**
     * Get page size if valid.
     *
     * @param pageSize String?
     * @return Int
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    private fun getPageSize(pageSize: String?) = run {
        var pageSizeAsInt = DEFAULT_PAGE_SIZE
        pageSize?.run {
            var issue = false
            try {
                pageSizeAsInt = pageSize.toInt()
                if (pageSizeAsInt < MIN_PAGE_SIZE || pageSizeAsInt > MAX_PAGE_SIZE)
                    issue = true
            } catch (e: NumberFormatException) {
                issue = true
            }

            if (issue) {
                throw BadRequestException("\"page_size must be between $MIN_PAGE_SIZE and $MAX_PAGE_SIZE\"")
            }
        }
        pageSizeAsInt
    }

    /**
     * Get page number if valid.
     *
     * @param pageNumber String?
     * @param numberOfPages Int
     * @return Int
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    private fun getPageNumber(pageNumber: String?, numberOfPages: Int) = run {
        var pageNumberAsInt = DEFAULT_PAGE_NUMBER
        pageNumber?.run {
            var issue = false
            try {
                pageNumberAsInt = pageNumber.toInt()
                if (pageNumberAsInt < MIN_PAGE_NUMBER || pageNumberAsInt > numberOfPages)
                    issue = true
            } catch (e: NumberFormatException) {
                issue = true
            }

            if (issue) {
                throw BadRequestException("page_number must be between $MIN_PAGE_NUMBER and $numberOfPages")
            }
        }
        pageNumberAsInt
    }

    /**
     * Get the epoch time from a string provided.
     *
     * @param dateStr String
     * @param fieldName String
     * @return Long
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    private fun getEpochFromDateString(dateStr: String, fieldName: String): Long {
        try {
            return sdf.parse(dateStr).time / 1000 // convert to secs from millisecs
        } catch (e: ParseException) {
            throw BadRequestException("Failed to parse $fieldName: $dateStr.  Format should be: $DATE_FORMAT.")
        }
    }

    companion object {
        private const val DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'"
        private val sdf = SimpleDateFormat(DATE_FORMAT)

        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 10000
        private const val DEFAULT_PAGE_NUMBER = 1

        private const val MIN_PAGE_NUMBER = 1
        private const val DEFAULT_PAGE_SIZE = 100

        private const val DEFAULT_SORT_ORDER = "asc"
    }
}