package gov.cdc.ocio.processingnotifications.workflow

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod


/**
 * The interface which defines the digest counts and top errors during an upload and its frequency
 */
@WorkflowInterface
interface UploadDigestCountsNotificationWorkflow {

    @WorkflowMethod
    fun processDailyUploadDigest(
        dataStreamIds: List<String>,
        dataStreamRoutes: List<String>,
        jurisdictions: List<String>,
        emailAddresses: List<String>
    )

}
