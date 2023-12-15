package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.UploadStatus
import gov.cdc.ocio.processingstatusapi.model.UploadsStatus
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

/**
 * Collection of ways to get upload status.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property context ExecutionContext
 * @constructor
 */
class GetUploadStatusFunction(
        private val request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext) {

    private val logger = context.logger

    private val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(context, reportsContainerName, partitionKey)!!
    }

    /**
     * Advanced query for dex uploads, including sorting, filtering and pagination.
     *
     * @param destinationId String
     * @param stageName String
     * @return HttpResponseMessage
     */
    fun uploadStatus(destinationId: String, stageName: String): HttpResponseMessage {

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
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
        sqlQuery.append("from $reportsContainerName t where t.destinationId = '$destinationId' and t.stageName = '$stageName'")

        extEvent?.run {
            sqlQuery.append(" and t.eventType = '$extEvent'")
        }

        dateStart?.run {
            try {
                val dateStartEpochSecs = getEpochFromDateString(dateStart, "date_start")
                sqlQuery.append(" and t._ts >= $dateStartEpochSecs")
            } catch (e: BadRequestException) {
                logger.log(Level.SEVERE, e.localizedMessage)
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
                logger.log(Level.SEVERE, e.localizedMessage)
                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(e.localizedMessage)
                        .build()
            }
        }

        val countQuery = "select value count(1) $sqlQuery"
        logger.info("upload status count query = $countQuery")

        var totalItems = 0L
        try {
            val count = reportsContainer.queryItems(
                    countQuery, CosmosQueryRequestOptions(),
                    Long::class.java
            )
            totalItems = if (count.count() > 0) count.first().toLong() else -1
            logger.info("Upload status matched count = $totalItems")
        } catch (ex: Exception) {
            // no items found or problem with query
            logger.warning(ex.localizedMessage)
        }

        val numberOfPages: Int
        val pageNumberAsInt: Int
        val reports: List<Report>
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

            sortBy?.run {
                val sortField = when (sortBy) {
                    "date" -> "_ts"
                    else -> {
                        return request
                                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                                .body("sort_by must be one of the following: [date]")
                                .build()
                    }
                }
                var sortOrderVal = DEFAULT_SORT_ORDER
                sortOrder?.run {
                    sortOrderVal = when (sortOrder) {
                        "ascending" -> "asc"
                        "descending" -> "desc"
                        else -> {
                            return request
                                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                                    .body("sort_order must be one of the following: [ascending, descending]")
                                    .build()
                        }
                    }
                }
                sqlQuery.append(" order by t.$sortField $sortOrderVal")
            }
            val offset = (pageNumberAsInt - 1) * pageSizeAsInt
            val dataSqlQuery = "select * $sqlQuery offset $offset limit $pageSizeAsInt"
            logger.info("upload status data query = $dataSqlQuery")
            reports = reportsContainer.queryItems(
                    dataSqlQuery, CosmosQueryRequestOptions(),
                    Report::class.java
            ).toList()
        } else {
            numberOfPages = 0
            pageNumberAsInt = 0
            reports = listOf()
        }

        val uploadsStatus = UploadsStatus()
        reports.forEach { report ->
            try {
               val uploadStatus = UploadStatus.createFromReport(report)
                uploadsStatus.items.add(uploadStatus)
            } catch (e: ContentException) {
                logger.warning("Unable to convert stage report with name, \"$stageName\" to upload status: ${e.localizedMessage}")
            }
        }

        uploadsStatus.summary.pageNumber = pageNumberAsInt
        uploadsStatus.summary.pageSize = pageSizeAsInt
        uploadsStatus.summary.numberOfPages = numberOfPages
        uploadsStatus.summary.totalItems = totalItems

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(uploadsStatus)
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