package gov.cdc.ocio.processingstatusapi.loaders

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.dao.ReportDao
import gov.cdc.ocio.processingstatusapi.models.DataStream
import gov.cdc.ocio.processingstatusapi.models.SortOrder
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
}
