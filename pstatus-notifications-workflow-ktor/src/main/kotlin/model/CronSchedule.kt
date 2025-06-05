package gov.cdc.ocio.processingnotifications.model

import java.time.OffsetDateTime

/**
 * Raw cron schedule and its human-readable form.
 *
 * @property cron String?
 * @property description String?
 * @property lastRun OffsetDateTime?
 * @property nextExecution String?
 * @constructor
 */
data class CronSchedule(
    val cron: String?,
    val description: String?,
    val lastRun: OffsetDateTime?,
    val nextExecution: String?
)