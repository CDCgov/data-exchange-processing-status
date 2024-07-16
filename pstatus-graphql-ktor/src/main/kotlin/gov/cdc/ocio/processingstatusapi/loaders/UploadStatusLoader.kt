package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.implementation.changefeed.common.ChangeFeedHelper
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.FeedResponse
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import gov.cdc.ocio.processingstatusapi.models.query.UploadStatus
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import gov.cdc.ocio.processingstatusapi.utils.DateUtils
import gov.cdc.ocio.processingstatusapi.utils.PageUtils
import kotlin.collections.set


class UploadStatusLoader: CosmosLoader() {

    /**
     * Advanced query for dex uploads, including sorting, filtering and pagination.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String?
     * @param dateStart String?
     * @param dateEnd String?
     * @param pageSize Int
     * @param pageNumber Int
     * @param sortBy String?
     * @param sortOrder String?
     * @param fileName String?
     * @return UploadsStatus
     * @throws BadRequestException
     * @throws ContentException
     * @throws BadStateException
     */
    @Throws(BadRequestException::class, ContentException::class, BadStateException::class)
    fun uploadStatus(dataStreamId: String,
                     dataStreamRoute: String?,
                     dateStart: String?,
                     dateEnd: String?,
                     pageSize: Int,
                     pageNumber: Int,
                     sortBy: String?,
                     sortOrder: String?,
                     fileName:String?
    ): UploadsStatus {

        logger.info("dataStreamId = $dataStreamId")

        val pageUtils = PageUtils.Builder()
            .setMinPageSize(MIN_PAGE_SIZE)
            .setMaxPageSize(MAX_PAGE_SIZE)
            .setDefaultPageSize(DEFAULT_PAGE_SIZE)
            .build()

        val pageSizeAsInt = pageUtils.getPageSize(pageSize)

        val sqlQuery = StringBuilder()
        sqlQuery.append("from $reportsContainerName t where t.dataStreamId = '$dataStreamId'")

        dataStreamRoute?.run {
            sqlQuery.append(" and t.dataStreamRoute = '$dataStreamRoute'")
        }

        fileName?.run {
            sqlQuery.append(" and t.content.filename = '$fileName'")
        }

        dateStart?.run {
            val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart, "date_start")
            sqlQuery.append(" and t._ts >= $dateStartEpochSecs")
        }
        dateEnd?.run {
            val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd, "date_end")
            sqlQuery.append(" and t._ts < $dateEndEpochSecs")
        }
        sqlQuery.append(" group by t.uploadId, t._ts")

        // Check the sort field as well
        sortBy.run {
            val sortField = when (sortBy) {
                //"date" -> "_ts"
                "filename" -> "content.filename"
                "dataStreamId" -> "destinationId"
                "dataStreamRoute" -> "eventType"
                "stageName" -> "stageName"
                else -> {
                    return@run
                }
            }
            //Add the sort by fields to grouping
            sqlQuery.append(" , t.$sortField")
            logger.info("sqlQuery = $sqlQuery")
        }


        // Query for getting counts in the structure of UploadCounts object.  Note the MAX aggregate which is used to
        // get the latest timestamp from t._ts.
        //val countQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery"
        val countQuery = "select count(1) as reportCounts $sqlQuery"
        logger.info("upload status count query = $countQuery")

        var totalItems = 0
        try {
            val count = reportsContainer?.queryItems(
                countQuery, CosmosQueryRequestOptions(),
                UploadCounts::class.java
            )
            totalItems = count?.count() ?: 0
            logger.info("Upload status matched count = $totalItems")
        } catch (ex: Exception) {
            // no items found or problem with query
            logger.error(ex.localizedMessage)
        }

        val numberOfPages: Int
        var pageNumberAsInt: Int
        var reports = mutableMapOf<String, List<ReportDao>>()
        if (totalItems > 0L) {
            numberOfPages =  (totalItems / pageSize + if (totalItems % pageSize > 0) 1 else 0)

            pageNumberAsInt = PageUtils.getPageNumber(pageNumber, numberOfPages)

            // order by not working
            sortBy?.run {
                val sortField = when (sortBy) {
                    "date" -> "_ts"
                    "filename" -> "content.filename"
                    "dataStreamId" -> "destinationId"
                    "dataStreamRoute" -> "eventType"
                    "stageName" -> "stageName"
                    else -> {
                        return@run
//                        return request
//                                .createResponseBuilder(HttpStatusCode.BadRequest)
//                                .body("sort_by must be one of the following: [date]")
//                                .build()

                    }
                }
                var sortOrderVal = DEFAULT_SORT_ORDER
                sortOrder?.run {
                    sortOrderVal = when (sortOrder) {
                        "ascending" -> "asc"
                        "descending" -> "desc"
                        else -> {
                            return@run
//                            return request
//                                    .createResponseBuilder(HttpStatusCode.BadRequest)
//                                    .body("sort_order must be one of the following: [ascending, descending]")
//                                    .build()
                        }
                    }
                }
                sqlQuery.append(" order by t.$sortField $sortOrderVal")
            }
            val offset = (pageNumberAsInt - 1) * pageSize
            val dataSqlQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery offset $offset limit $pageSizeAsInt"

            //val dataSqlQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery"
            logger.info("upload status data query = $dataSqlQuery")

//            val options = CosmosQueryRequestOptions()
//            // 0 maximum parallel tasks, effectively serial execution
//            options.setMaxDegreeOfParallelism(0)
//            options.setMaxBufferedItemCount(100)
//            reports = queryWithPagingAndContinuationTokenAndPrintQueryCharge(options, dataSqlQuery, pageSize, pageNumberAsInt)








            //OLD - Commented for Pagination
            val results = reportsContainer?.queryItems(
                dataSqlQuery, CosmosQueryRequestOptions(),
                UploadCounts::class.java
            )?.toList()

            results?.forEach { report ->
                val uploadId = report.uploadId
                    ?: throw BadStateException("Upload ID unexpectedly null")
                val reportsSqlQuery = "select * from $reportsContainerName t where t.uploadId = '$uploadId'"
                logger.info("get reports for upload query = $reportsSqlQuery")
                val reportsForUploadId = reportsContainer?.queryItems(
                    reportsSqlQuery, CosmosQueryRequestOptions(),
                    ReportDao::class.java
                )?.toList()

                reportsForUploadId?.let { reports[uploadId] = reportsForUploadId }
            }


        } else {
            numberOfPages = 0
            pageNumberAsInt = 0
        }

        val uploadsStatus = UploadsStatus()
        reports.forEach { report ->
            val uploadStatus = UploadStatus.createFromReports(uploadId = report.key, reports = report.value)
            uploadsStatus.items.add(uploadStatus)
        }

        uploadsStatus.summary.pageNumber = pageNumberAsInt
        uploadsStatus.summary.pageSize = pageSize
        uploadsStatus.summary.numberOfPages = numberOfPages
        uploadsStatus.summary.totalItems = totalItems

        return uploadsStatus
    }

    companion object {
        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 10000
        const val DEFAULT_PAGE_SIZE = 100

        private const val DEFAULT_SORT_ORDER = "asc"
    }

    @Throws(Exception::class)
    fun queryWithPagingAndContinuationTokenAndPrintQueryCharge(options: CosmosQueryRequestOptions,
                                                               query: String,
                                                               pageSize: Int,
                                                               pageNumber: Int): MutableMap<String, List<ReportDao>> {
        logger.info("Query with paging and continuation token");

        options.feedRange

        //Create query request with continuation token
        var currentPageNumber = pageNumber
        var continuationToken: String? = null
        var documentNumber = 0
        var requestCharge = 0.0

        val reports = mutableMapOf<String, List<ReportDao>>()

        // First iteration (continuationToken = null): Receive a batch of query response pages
        // Subsequent iterations (continuationToken != null): Receive subsequent batch of query response pages,
        // with continuationToken indicating where the previous iteration left off
        do {
            logger.info("Receiving a set of query response pages.")
            logger.info("Continuation Token: $continuationToken\n")

            val feedResponseIterator: MutableIterable<FeedResponse<UploadCounts>>? =
                reportsContainer?.queryItems(
                    query, CosmosQueryRequestOptions(),
                    UploadCounts::class.java
                )?.iterableByPage(continuationToken, pageSize)

            if (feedResponseIterator != null) {
                for (page in feedResponseIterator) {
                    logger.info(java.lang.String.format("Current page number: %d", currentPageNumber))
//                        // Access all the documents in this result page
//                        for (results in page.results) {
//                            documentNumber++
//
//                        }


                    // Access all the documents in this result page
                    page.results?.forEach { report ->
                        val uploadId = report.uploadId
                            ?: throw BadStateException("Upload ID unexpectedly null")
                        val reportsSqlQuery = "select * from $reportsContainerName t where t.uploadId = '$uploadId'"
                        logger.info("get reports for upload query = $reportsSqlQuery")
                        val reportsForUploadId = reportsContainer?.queryItems(
                            reportsSqlQuery, CosmosQueryRequestOptions(),
                            ReportDao::class.java
                        )?.toList()

                        reportsForUploadId?.let { reports[uploadId] = reportsForUploadId }
                    }

                    // Accumulate the request charge of this page
                    requestCharge += page.requestCharge

                    // Page count so far
                    logger.info(java.lang.String.format("Total documents received so far: %d", documentNumber))

                    // Request charge so far
                    logger.info(java.lang.String.format("Total request charge so far: %f\n", requestCharge))

                    // Along with page results, get a continuation token
                    // which enables the client to "pick up where it left off"
                    // in accessing query response pages.
                    continuationToken = page.continuationToken

                    currentPageNumber++
                }
            }
        } while (continuationToken != null)

        return reports

    }
}