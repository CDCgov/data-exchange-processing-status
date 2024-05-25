package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Collection of duplicate uploaded filenames found")
data class DuplicateFilenameCounts(

    @GraphQLDescription("Filename of the file that is duplicated.")
    var filename: String? = null,

    @GraphQLDescription("Total number of times the duplicate filename was found, which will always be 2 or more.")
    var totalCount: Long = 0
)