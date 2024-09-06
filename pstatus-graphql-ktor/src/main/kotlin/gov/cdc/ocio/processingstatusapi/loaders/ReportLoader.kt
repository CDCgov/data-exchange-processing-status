package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.DataStream
import gov.cdc.ocio.processingstatusapi.models.SortOrder
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.submission.RollupStatus
import gov.cdc.ocio.processingstatusapi.models.submission.Status
import gov.cdc.ocio.processingstatusapi.models.submission.SubmissionDetails
import graphql.schema.DataFetchingEnvironment
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

class ForbiddenException(message: String) : RuntimeException(message)


/**
 * Report loader for graphql
 */
class ReportLoader: CosmosLoader() {

    /**
     * Get all reports associated with the provided upload id.
     *
     * @param dataFetchingEnvironment DataFetchingEnvironment
     * @param uploadId String
     * @param reportsSortedBy String?
     * @param sortOrder SortOrder?
     * @return List<Report>
     */
    fun getByUploadId(dataFetchingEnvironment: DataFetchingEnvironment,
                      uploadId: String,
                      reportsSortedBy: String?,
                      sortOrder: SortOrder?
    ) = getReportsForUploadId(dataFetchingEnvironment, uploadId, reportsSortedBy, sortOrder)

    /**
     * Submission details contain all the known details for a particular upload.
     * It provides a roll-up of all the reports associated with the upload as well as some summary information..
     *
     * @param dataFetchingEnvironment DataFetchingEnvironment
     * @param uploadId String
     * @param reportsSortedBy String?
     * @param sortOrder SortOrder?
     * @return UploadDetails
     */
    fun getSubmissionDetailsByUploadId(dataFetchingEnvironment: DataFetchingEnvironment,
                                       uploadId: String,
                                       reportsSortedBy: String?,
                                       sortOrder: SortOrder?
    ): SubmissionDetails {

        val reports = getReportsForUploadId(dataFetchingEnvironment, uploadId, reportsSortedBy, sortOrder)

        return getUploadDetails(reports)
    }

    /**
     * Search for reports with the provided ids.
     *
     * @param ids List<String>
     * @return List<Report>
     */
    fun search(ids: List<String>): List<Report> {
        val quotedIds = ids.joinToString("\",\"", "\"", "\"")

        val reportsSqlQuery = "select * from r where r.id in ($quotedIds)"

        val reportItems = reportsContainer?.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            ReportDao::class.java
        )
        val reports = mutableListOf<Report>()
        reportItems?.forEach { reports.add(it.toReport()) }

        return reports
    }

    /**
     * Get all reports associated with the provided upload id.
     *
     * @param dataFetchingEnvironment DataFetchingEnvironment
     * @param uploadId String
     * @param reportsSortedBy String?
     * @param sortOrder SortOrder?
     * @return List<Report>
     */
    private fun getReportsForUploadId(dataFetchingEnvironment: DataFetchingEnvironment,
                                      uploadId: String,
                                      reportsSortedBy: String?,
                                      sortOrder: SortOrder?
    ): List<Report> {

        // Obtain the data streams available to the user from the data fetching env.
        val authContext = dataFetchingEnvironment.graphQlContext.get<AuthenticationContext>("AuthContext")
        var dataStreams: List<DataStream>? = null
        authContext?.run {
            val principal = authContext.principal<JWTPrincipal>()
            dataStreams = principal?.payload?.getClaim("dataStreams")?.asList(DataStream::class.java)
        }

        val reportsSqlQuery = StringBuilder()
        reportsSqlQuery.append("select * from r where r.uploadId = '$uploadId'")

        when (reportsSortedBy) {
            "timestamp" -> {
                val sortOrderVal = when (sortOrder) {
                    SortOrder.Ascending -> "asc"
                    SortOrder.Descending -> "desc"
                    else -> "asc" // default
                }
                reportsSqlQuery.append(" order by r.timestamp $sortOrderVal")
            }
            null -> {
                // nothing to sort by
            }
            else -> {
                throw BadRequestException("Reports can not be sorted by '$reportsSortedBy'")
            }
        }
        val reportItems = reportsContainer?.queryItems(
            reportsSqlQuery.toString(), CosmosQueryRequestOptions(),
            ReportDao::class.java
        )

        // Convert the report DAOs to reports and ensure the user has access to them.
        val reports = mutableListOf<Report>()
        reportItems?.forEach { reportItem ->
            val report = reportItem.toReport()
            dataStreams?.run {
                if (dataStreams?.firstOrNull { ds -> ds.name == report.dataStreamId && ds.route == report.dataStreamRoute } == null)
                    throw ForbiddenException("You are not allowed to access this resource.")
            }
            reports.add(report)
        }

        return reports
    }

    /**
     *  Get uploadDetails
     *  @param reports MutableList<Report>
     *  @return UploadDetails
     */

    private fun getUploadDetails(reports: List<Report>): SubmissionDetails {

        // Determine rollup status
        val rollupStatus = when {
            reports.all { it.stageInfo?.status == Status.SUCCESS } -> RollupStatus.DELIVERED
            reports.any { it.stageInfo?.status == Status.FAILURE } -> RollupStatus.FAILED
            reports.isNotEmpty() -> RollupStatus.PROCESSING
            else -> null
        }

        // Find report with most recent timestamp
        val lastReport = reports.maxByOrNull { it.timestamp!! }
        val firstReport = reports.firstOrNull()
        val stageInfo = lastReport?.stageInfo
        // Find the first report with service "upload" and action "upload-status"
        val uploadStatusReport = reports.firstOrNull { it.stageInfo?.service == "upload" && it.stageInfo?.action == "upload-status" }
        // Retrieve the value associated with "outerKey2" and cast it to LinkedHashMap
        val retrievedInnerMap: LinkedHashMap<*, *>? = uploadStatusReport?.content?.get("report") as? LinkedHashMap<*, *>?
        val fileName = retrievedInnerMap?.get("received_filename")

        return SubmissionDetails(
            status = rollupStatus.toString(),
            lastService = stageInfo?.service,
            lastAction =stageInfo?.action,
            filename = fileName?.toString(),
            uploadId = lastReport?.uploadId,
            dexIngestDateTime = firstReport?.timestamp,
            dataStreamId = firstReport?.dataStreamId,
            dataStreamRoute = firstReport?.dataStreamRoute,
            jurisdiction = firstReport?.jurisdiction,
            senderId = firstReport?.senderId,
            dataProducerId= firstReport?.dataProducerId,
            reports = reports
        )
    }
}
