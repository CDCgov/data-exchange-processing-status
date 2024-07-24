package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import gov.cdc.ocio.processingstatusapi.models.query.UploadStatus
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import gov.cdc.ocio.processingstatusapi.utils.DateUtils
import gov.cdc.ocio.processingstatusapi.utils.PageUtils
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary
import kotlinx.coroutines.runBlocking

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
    fun getUploadStatus(dataStreamId: String,
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

        //Create SQL Query String
        val sqlQuery = StringBuilder()
        sqlQuery.append(" from $reportsContainerName t")

        // Create SQL parameter list with all the filter fields
        val paramList = ArrayList<SqlParameter>()

        dataStreamId.run {
            paramList.add(SqlParameter("@dataStreamId", dataStreamId))
            sqlQuery.append(" where t.dataStreamId = @dataStreamId")
        }
        dataStreamRoute?.run {
            paramList.add(SqlParameter("@dataStreamRoute", dataStreamRoute))
            sqlQuery.append(" and t.dataStreamRoute = @dataStreamRoute")
        }
        fileName?.run {
            paramList.add(SqlParameter("@fileName", fileName))
            sqlQuery.append(" and t.content.filename = @fileName")
        }

        dateStart?.run {
            val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart, "date_start")
            paramList.add(SqlParameter("@dateStart", dateStartEpochSecs))
            sqlQuery.append(" and t._ts >= @dateStart")
        }
        dateEnd?.run {
            val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd, "date_end")
            paramList.add(SqlParameter("@dateEnd", dateEndEpochSecs))
            sqlQuery.append(" and t._ts < @dateEnd")
        }
        sqlQuery.append(" group by t.uploadId, t._ts")

        // Check the sort field as well to add them to the group by clause
        sortBy.run {
            val sortField = when (sortBy) {
                //"date" -> "_ts" // "group  by _ts" is already added by default above
                "fileName" -> "content.filename"
                "dataStreamId" -> "destinationId"
                "dataStreamRoute" -> "eventType"
                "stageName" -> "stageName"
                else -> {
                    return@run
                }
            }
            //Add the sort by fields to grouping
            sqlQuery.append(" , t.$sortField")
            logger.info("Upload Status, sqlQuery = $sqlQuery")
        }

        // Query for getting counts in the structure of UploadCounts object.  Note the MAX aggregate which is used to
        // get the latest timestamp from t._ts.
        //val countQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery"
        val countQuery = "select count(1) as reportCounts $sqlQuery"
        logger.info("upload status, countQuery = $countQuery")

        var totalItems = 0

        //Create items to query the Cosmos Container

        //Create QuerySpec
        val querySpec = SqlQuerySpec(countQuery, paramList)

        //Create CosmosQueryRequestOptions and set the partitionKey
        val options = CosmosQueryRequestOptions()
        options.setMaxDegreeOfParallelism(-1)
        options.setMaxBufferedItemCount(pageSize)

        try{
            val count = reportsContainer?.queryItems(
                querySpec,
                options,
                UploadCounts::class.java)
            totalItems = count?.count() ?: 0
            logger.info("Upload status matched count = $totalItems")
        }catch (ex: Exception) {
            // no items found or problem with query
            logger.error(ex.localizedMessage)
        }

        // If there is data
        val numberOfPages: Int
        val pageNumberAsInt: Int
        var reports = mutableMapOf<String, MutableList<ReportDao>>()

        if (totalItems > 0L) {
            numberOfPages =  (totalItems / pageSize + if (totalItems % pageSize > 0) 1 else 0)
            pageNumberAsInt = PageUtils.getPageNumber(pageNumber, numberOfPages)

            // order by
            sortBy?.run {
                val sortField = when (sortBy) {
                    "date" -> "_ts"
                    "fileName" -> "content.filename"
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
//            val offset = (pageNumberAsInt - 1) * pageSize
            //        val dataSqlQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery offset $offset limit $pageSizeAsInt"
            val dataSqlQuery = "select count(1) as reportCounts, t.uploadId, MAX(t._ts) as latestTimestamp $sqlQuery"
            logger.info("upload status data query = $dataSqlQuery")

            val querySpecResults = SqlQuerySpec(dataSqlQuery, paramList)

            var results: List<UploadCounts> = emptyList()
            //  Sync API
            val filteredUploads =
                reportsContainer?.queryItems(querySpecResults, options, UploadCounts::class.java)
                    ?.iterableByPage(null, pageSize)

            // Use Skip Counts to get the exact number of results to be skipped
            val skipCount = pageSize * (pageNumber - 1)
            var count = 0
            var continuationToken: String

            if (filteredUploads != null) {
                for (page in filteredUploads) {
                    if (count < skipCount) {
                        count += page.results.size
                        continue // Skip items until we reach the desired page
                    }
                    results = page.results.toList()
                    logger.info("Page request charge:: " + page.requestCharge)
                    if (results.size >= pageSize) {
                        break // Stop if we've collected enough items
                    }
                    // continuationToken = page.continuationToken
                }
            }

            //Optimizing
            // Batch processing to improve performance
            reports = fetchReports(results, pageSize, skipCount, paramList)

            //Testing with ContinuationToken
//            val options = CosmosQueryRequestOptions()
//            // 0 maximum parallel tasks, effectively serial execution
//            options.setMaxDegreeOfParallelism(0)
//            options.setMaxBufferedItemCount(100)
//            reports = queryWithPagingAndContinuationTokenAndPrintQueryCharge(options, dataSqlQuery, pageSize, pageNumberAsInt)


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

    /*
    * Fetch the associated reports for the uploads matching the current request
    *
    * */
    private fun fetchReports(
        results: List<UploadCounts>,
        pageSize: Int,
        skipCount: Int,
        paramList: ArrayList<SqlParameter>
    ): MutableMap<String, MutableList<ReportDao>> {
        val uploadIds = results.map { it.uploadId }
        val reportsMap = mutableMapOf<String, MutableList<ReportDao>>() // Use MutableList for efficient appending

        val options = CosmosQueryRequestOptions()
        options.setMaxDegreeOfParallelism(0)
        options.setMaxBufferedItemCount(pageSize)

        runBlocking {
            val batches = uploadIds.chunked(20)
            for (batch in batches) {
                val parameters = batch.mapIndexed { index, id -> SqlParameter("@uploadId$index", id) }
                val paramPlaceholders = batch.indices.joinToString(", ") { "@uploadId$it" }
                val allReportsSqlQuery = """
                SELECT *
                FROM c
                WHERE c._ts >= @dateStart AND c.dataStreamId = @dataStreamId AND c.uploadId IN ($paramPlaceholders)
            """.trimIndent()

                paramList.addAll(parameters)

                val subQuerySpec = SqlQuerySpec(allReportsSqlQuery, paramList)
                logger.info("Processing in batches, subQuerySpec = $subQuerySpec")

                // Query and process pages asynchronously
                val filteredReports = reportsContainer?.queryItems(subQuerySpec, options, ReportDao::class.java)
                val pageResults = filteredReports?.iterableByPage()

                var count = 0
                pageResults?.forEach { page ->
                    if (count < skipCount) {
                        count += page.results.size
                        return@forEach // Skip until we reach the desired page
                    }

                    // Process the reports in this page
                    page.results
                        .filter { it.uploadId != null }
                        .groupBy { it.uploadId!! }
                        .forEach { (uploadId, reportList) ->
                            reportsMap.getOrPut(uploadId) { mutableListOf() }.addAll(reportList)
                        }
                }
            }
        }
        return reportsMap
    }

}

