package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.ReportSchemaLoader


class ReportSchemaQueryService : Query {

    @GraphQLDescription("Provides the schema loader information")
    @Suppress("unused")
    fun schemaLoaderInfo() = ReportSchemaLoader().loaderInfo()

    @GraphQLDescription("Provides a list of all the available report schemas")
    @Suppress("unused")
    fun listReportSchemas() = ReportSchemaLoader().list()
}