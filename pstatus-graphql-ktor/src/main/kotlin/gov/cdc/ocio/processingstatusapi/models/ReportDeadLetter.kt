package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Report for a given stage.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property contentType String?
 * @property messageId String?
 * @property status String?
 * @property content String?
 * @property timestamp OffsetDateTime
 */
@GraphQLDescription("Contains Report DeadLetter content.")
class ReportDeadLetter: Report()