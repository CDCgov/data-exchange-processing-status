package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.models.reports.BaseContent

/**
 * Report for a given stage.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property stageName String?
 * @property contentType String?
 * @property messageId String?
 * @property status String?
 * @property content String?
 * @property timestamp Date
 */
@GraphQLDescription("Contains Report content.")
data class Report(

    var id : String? = null,

    var uploadId: String? = null,

    var reportId: String? = null,

    var dataStreamId: String? = null,

    var dataStreamRoute: String? = null,

    var stageName: String? = null,

    var contentType : String? = null,

    var messageId: String? = null,

    var status : String? = null,

    var content: BaseContent? = null,

    var timestamp: Float? = null // TODO: Date
)