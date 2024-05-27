package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.ReportCounts
import gov.cdc.ocio.processingstatusapi.models.query.PageSummary

/**
 * Aggregate report counts
 *
 * @property summary PageSummary?
 * @property reportCountsList List<ReportCounts>?
 * @constructor
 */
@GraphQLDescription("Aggregate report counts")
data class AggregateReportCounts(

    @GraphQLDescription("Page summary for the counts provided")
    var summary: PageSummary? = null,

    @GraphQLDescription("List of the report counts")
    var uploads: List<ReportCounts>? = null
)