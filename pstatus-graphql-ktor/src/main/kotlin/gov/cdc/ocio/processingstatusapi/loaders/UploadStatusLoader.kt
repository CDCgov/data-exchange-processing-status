package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.query.UploadStatus
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import gov.cdc.ocio.processingstatusapi.utils.DateUtils
import gov.cdc.ocio.processingstatusapi.utils.PageUtils

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

        val sqlQuery = StringBuilder()
        sqlQuery.append("from r where r.dataStreamId = '$dataStreamId'")

        dataStreamRoute?.run {
            sqlQuery.append(" and r.dataStreamRoute = '$dataStreamRoute'")
        }

        fileName?.run {
            sqlQuery.append(" and r.content.filename = '$fileName'")
        }

        dateStart?.run {
            val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart, "date_start")
            sqlQuery.append(" and r._ts >= $dateStartEpochSecs")
        }
        dateEnd?.run {
            val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd, "date_end")
            sqlQuery.append(" and r._ts < $dateEndEpochSecs")
        }

        sqlQuery.append(" group by r.uploadId")

        // Check the sort field as well to add them to the group by clause
        sortBy.run {
            val sortField = when (sortBy) {
                //"date" -> "_ts" // "group  by _ts" is already added by default above
                "fileName" -> "content.filename"
                "dataStreamId" -> "destinationId"
                "dataStreamRoute" -> "eventType"
                "stageName" -> "stageName"
                "jurisdiction" -> "content.metadata.meta_ext_source"
                else -> {
                    return@run
                }
            }
            //Add the sort by fields to grouping
            sqlQuery.append(" , r.$sortField, r.content.metadata.meta_ext_entity, r.content.metadata.meta_username")
        }

        logger.info("Upload Status, sqlQuery = $sqlQuery")
        // Query for getting counts in the structure of UploadCounts object.  Note the MAX aggregate which is used to
        // get the latest timestamp from t._ts.
        val countQuery = "select count(1) as reportCounts, r.uploadId, MAX(r._ts) as latestTimestamp, r.content.metadata.meta_ext_entity as jurisdiction, r.content.metadata.meta_username as senderId $sqlQuery"
        logger.info("upload status count query = $countQuery")

        var totalItems = 0
        var jurisdictions:List<String> = listOf()
        var senderIds:List<String> = listOf()

        try {
            val count = reportsContainer?.queryItems(
                countQuery, CosmosQueryRequestOptions(),
                UploadCounts::class.java
            )
            totalItems = count?.count() ?: 0
            logger.info("Upload status matched count = $totalItems")

            jurisdictions = count?.toList()?.mapNotNull { it.jurisdiction }?.toSet()?.toList() ?: listOf()
            senderIds = count?.toList()?.mapNotNull { it.senderId }?.toSet()?.toList() ?: listOf()

        } catch (ex: Exception) {
            // no items found or problem with query
            logger.error(ex.localizedMessage)
        }


        val numberOfPages: Int
        val pageNumberAsInt: Int
        val reports = mutableMapOf<String, List<ReportDao>>()
        if (totalItems > 0L) {
            numberOfPages =  (totalItems / pageSize + if (totalItems % pageSize > 0) 1 else 0)

            pageNumberAsInt = PageUtils.getPageNumber(pageNumber, numberOfPages)

            sortBy?.run {
                val sortField = when (sortBy) {
                    "date" -> "_ts"
                    "fileName" -> "content.filename"
                    "dataStreamId" -> "destinationId"
                    "dataStreamRoute" -> "eventType"
                    "stageName" -> "stageName"
                    "jurisdiction" -> "content.metadata.meta_ext_source"
                    else -> {
                        return@run
                        /* return request
                                 .createResponseBuilder(HttpStatus.BAD_REQUEST)
                                 .body("sort_by must be one of the following: [date]")
                                 .build()*/
                    }
                }
                var sortOrderVal = DEFAULT_SORT_ORDER
                sortOrder?.run {
                    sortOrderVal = when (sortOrder) {
                        "ascending" -> "asc"
                        "descending" -> "desc"
                        else -> {
                            return@run
                            /*return request
                                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                                    .body("sort_order must be one of the following: [ascending, descending]")
                                    .build()*/
                        }
                    }
                }
                sqlQuery.append(" order by r.$sortField $sortOrderVal")
            }
            val offset = (pageNumberAsInt - 1) * pageSize
            val dataSqlQuery = "select count(1) as reportCounts, r.uploadId, MAX(r._ts) as latestTimestamp, r.content.metadata.meta_ext_entity as jurisdiction, r.content.metadata.meta_username as senderId $sqlQuery offset $offset limit $pageSizeAsInt"
            logger.info("upload status data query = $dataSqlQuery")
            val results = reportsContainer?.queryItems(
                dataSqlQuery, CosmosQueryRequestOptions(),
                UploadCounts::class.java
            )?.toList()


            results?.forEach { report ->
                val uploadId = report.uploadId
                    ?: throw BadStateException("Upload ID unexpectedly null")
                val reportsSqlQuery = "select * from r where r.uploadId = '$uploadId'"
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
        uploadsStatus.summary.senderIds = senderIds
        uploadsStatus.summary.jurisdictions = jurisdictions

        return uploadsStatus
    }

    companion object {
        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 10000
        const val DEFAULT_PAGE_SIZE = 100

        private const val DEFAULT_SORT_ORDER = "asc"
    }
}