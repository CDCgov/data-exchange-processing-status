package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCounts
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadMetrics

data class UploadDigest(val counts: UploadDigestCounts, val metrics: UploadMetrics, val durations: List<Long>)
