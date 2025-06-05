package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.query.UploadStatus
import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus
import gov.cdc.ocio.database.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.query.UploadCounts
import gov.cdc.ocio.processingstatusapi.utils.PageUtils
import gov.cdc.ocio.processingstatusapi.utils.SortUtils
import gov.cdc.ocio.types.utils.DateUtils
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class UploadStatusLoader: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val reportsCollection = repository.reportsCollection

    private val cName = reportsCollection.collectionNameForQuery
    private val cVar = reportsCollection.collectionVariable
    private val cPrefix = reportsCollection.collectionVariablePrefix
    private val cElFunc = repository.reportsCollection.collectionElementForQuery
    private val timeFunc = repository.reportsCollection.timeConversionForQuery

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
    fun getUploadStatus(
        dataStreamId: String,
        dataStreamRoute: String?,
        dateStart: String?,
        dateEnd: String?,
        pageSize: Int,
        pageNumber: Int,
        sortBy: String?,
        sortOrder: String?,
        fileName: String?
    ): UploadsStatus {

        logger.info("dataStreamId = $dataStreamId")

        val pageUtils = PageUtils.Builder()
            .setMinPageSize(MIN_PAGE_SIZE)
            .setMaxPageSize(MAX_PAGE_SIZE)
            .setDefaultPageSize(DEFAULT_PAGE_SIZE)
            .build()

        val pageSizeAsInt = pageUtils.getPageSize(pageSize)

        val sqlQuery = StringBuilder()
        sqlQuery.append("from $cName $cVar where ${cPrefix}dataStreamId = '$dataStreamId'")

        dataStreamRoute?.run {
            sqlQuery.append(" and ${cPrefix}dataStreamRoute = '$dataStreamRoute'")
        }

        fileName?.run {
            sqlQuery.append(" and ${cPrefix}content.filename = '$fileName'")
        }

        dateStart?.run {
            val dateStartEpochMillis = timeFunc(DateUtils.getEpochFromDateString(dateStart))
            sqlQuery.append(" and ${cPrefix}dexIngestDateTime >= $dateStartEpochMillis")
        }
        dateEnd?.run {
            val dateEndEpochMillis = timeFunc(DateUtils.getEpochFromDateString(dateEnd))
            sqlQuery.append(" and ${cPrefix}dexIngestDateTime < $dateEndEpochMillis")
        }

        sqlQuery.append(" group by ${cPrefix}uploadId, ${cPrefix}jurisdiction, ${cPrefix}senderId")

        // Check the sort field as well to add them to the group by clause
        val sortByQueryStr = StringBuilder()
        sortBy.run {
            if (sortBy != null) {
                if (sortBy.isNotEmpty()) {
                    val sortField = when (sortBy) {
                        "date" -> "dexIngestDateTime" // "group  by _ts" is already added by default above
                        "fileName" -> "content.filename"
                        "dataStreamId" -> "dataStreamId"
                        "dataStreamRoute" -> "dataStreamRoute"
                        "service" -> "stageInfo.${cElFunc("service")}"
                        "action" -> "stageInfo.${cElFunc("action")}"
                        "jurisdiction" -> "jurisdiction"
                        "status" -> "stageInfo.status"
                        else -> {
                            throw BadRequestException("Invalid sort field: $sortBy")
                        }
                    }
                    //Add the sort by fields to grouping
                    sqlQuery.append(" , ${cPrefix}$sortField")

                    var sortOrderVal = DEFAULT_SORT_ORDER

                    sortOrder?.run {
                        if (sortOrder.isNotEmpty()) {
                            sortOrderVal = when (sortOrder.lowercase()) {
                                "asc", "ascending" -> "asc"
                                "desc", "descending" -> "desc"
                                else -> {
                                    throw BadRequestException("Invalid sort order: $sortOrder")
                                }
                            }
                        }

                    }
                    //Sort By/ Order By the given sort field
                    sortByQueryStr.append(" order by ${cPrefix}$sortField $sortOrderVal")
                }
            }
        }

        logger.info("Upload Status, sqlQuery = $sqlQuery")
        // Query for getting counts in the structure of UploadCounts object.  Note the MAX aggregate which is used to
        // get the latest timestamp from t._ts.
        val countQuery = (
                "select count(1) as reportCounts, ${cPrefix}uploadId, "
                        + "MAX(${cPrefix}dexIngestDateTime) as latestTimestamp, "
                        + "${cPrefix}jurisdiction as jurisdiction, "
                        + "${cPrefix}senderId as senderId $sqlQuery"
                )
        logger.info("upload status count query = $countQuery")

        var totalItems = 0
        var jurisdictions: List<String> = listOf()
        var senderIds: List<String> = listOf()

        try {
            val count = reportsCollection.queryItems(
                countQuery,
                UploadCounts::class.java
            )
            totalItems = count.count()
            logger.info("Upload status matched count = $totalItems")

            jurisdictions = count.toList().mapNotNull { it.jurisdiction }.toSet().toList()
            senderIds = count.toList().mapNotNull { it.senderId }.toSet().toList()

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

            //Add the sortBy query
            sqlQuery.append(sortByQueryStr)

            val offset = (pageNumberAsInt - 1) * pageSize
            val dataSqlQuery = (
                    "select count(1) as reportCounts, ${cPrefix}uploadId, "
                            + "MAX(${cPrefix}dexIngestDateTime) as latestTimestamp, "
                            + "${cPrefix}jurisdiction as jurisdiction, ${cPrefix}senderId as senderId "
                            + "$sqlQuery offset $offset limit $pageSizeAsInt"
                    )
            logger.info("upload status data query = $dataSqlQuery")
            val results = reportsCollection.queryItems(
                dataSqlQuery,
                UploadCounts::class.java
            ).toList()

            results.forEach { report ->
                val uploadId = report.uploadId
                    ?: throw BadStateException("Upload ID unexpectedly null")
                val reportsSqlQuery = "select * from $cName $cVar where ${cPrefix}uploadId = '$uploadId'"
                logger.info("get reports for upload query = $reportsSqlQuery")
                val reportsForUploadId = reportsCollection.queryItems(
                    reportsSqlQuery,
                    ReportDao::class.java
                ).toList()

                reportsForUploadId.let { reports[uploadId] = reportsForUploadId }
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
        uploadsStatus.summary.senderIds = senderIds.toMutableList()
        uploadsStatus.summary.jurisdictions = jurisdictions.toMutableList()

        if(sortOrder.isNullOrEmpty()){
            SortUtils.sortByField(uploadsStatus, sortBy.toString(), DEFAULT_SORT_ORDER)
        }else{
            SortUtils.sortByField(uploadsStatus, sortBy.toString(), sortOrder.toString())
        }

        return uploadsStatus
    }

    companion object {
        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 10000
        const val DEFAULT_PAGE_SIZE = 100

        private const val DEFAULT_SORT_ORDER = "desc"
    }
}