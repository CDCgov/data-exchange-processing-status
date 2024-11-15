package gov.cdc.ocio.processingstatusapi.models.dao

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean

/**
 * Represents the state of an upload operation in the system.
 *
 * @property uploadId Unique identifier for the upload
 * @property action The type of action performed (e.g., 'upload-completed', 'blob-file-copy', 'metadata-verify')
 * @property status The status of the action (e.g., 'SUCCESS', 'FAILURE')
 *
 * This data class is primarily used to:
 * 1. Map database query results for upload operations
 * 2. Track the progress and state of individual upload actions
 * 3. Support analysis of upload completion and delivery status
 *
 * Note: All properties are nullable to support partial database mappings
 */
@DynamoDbBean
data class UploadDao(
    var uploadId: String? = null,
    var action: String? = null,
    var status: String? = null
)