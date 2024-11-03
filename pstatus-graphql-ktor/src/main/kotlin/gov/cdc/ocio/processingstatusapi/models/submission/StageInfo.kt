package gov.cdc.ocio.processingstatusapi.models.submission

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Status of Report-SUCCESS OR FAILURE
 */
enum class Status {

    @GraphQLDescription("Success")
    SUCCESS,

    @GraphQLDescription("Failure")
    FAILURE
}

/**
 * Rollup status of Report-DELIVERED, FAILED OR PROCESSING
 */
enum class RollupStatus {

    @GraphQLDescription("Delivered")
    DELIVERED,

    @GraphQLDescription("Failed")
    FAILED,

    @GraphQLDescription("Processing")
    PROCESSING
}


/**
 * Message stageInfo within a Report.
 **
 * @property service String?
 * @property action String?
 * @property version String?
 * @property status String?
 * @property startProcessingTime Date?
 * @property endProcessingTime Date?
 */
@GraphQLDescription("Contains information about report service, action, version and whether it was a success or failure and the processing start and end times ")
data class StageInfo(

    @GraphQLDescription("Service")
    var service : String? = null,

    @GraphQLDescription("Stage name a.k.a action")
    var action: String? = null,

    @GraphQLDescription("Version")
    var version: String? = null,

    @GraphQLDescription("Status- SUCCESS OR FAILURE")
    var status: Status? = null,

    @GraphQLDescription("Issues array")
    var issues: List<Issue>? = null,

    @GraphQLDescription("Start processing time")
    var startProcessingTime: OffsetDateTime? = null,

    @GraphQLDescription("End processing time")
    var endProcessingTime: OffsetDateTime? = null

) {
    companion object {

        /**
         * Convenience function to convert a database object to a StageInfo object
         */
        fun fromStageInfoDao(dao: gov.cdc.ocio.database.models.dao.StageInfoDao?) = StageInfo().apply {
            this.service = dao?.service
            this.action = dao?.action
            this.version = dao?.version
            this.status = when (dao?.status) {
                gov.cdc.ocio.database.models.Status.SUCCESS -> Status.SUCCESS
                gov.cdc.ocio.database.models.Status.FAILURE -> Status.FAILURE
                else -> null
            }
            this.issues = dao?.issues?.map { Issue.fromIssueDao(it) }
            this.startProcessingTime = dao?.startProcessingTime?.atOffset(ZoneOffset.UTC)
            this.endProcessingTime =  dao?.endProcessingTime?.atOffset(ZoneOffset.UTC)
        }
    }
}