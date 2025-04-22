package gov.cdc.ocio.processingnotifications.utils

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull


object CronUtils {
    /**
     * Returns a human-readable version of the provided cron schedule (UNIX format).
     *
     * @param cronExpression String
     * @return String
     */
    fun description(cronExpression: String?): String? {
        if (cronExpression.isNullOrEmpty()) return null

        // Parse cronExpression expression and get description
        val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
        val descriptor = CronDescriptor.instance(Locale.US)
        return try {
            descriptor.describe(parser.parse(cronExpression))
        } catch (e: IllegalArgumentException) {
            cronExpression
        }
    }

    /**
     * Gets the next execution time from the provided cron expression.
     *
     * @param cronExpression String
     * @return Instant?
     */
    fun nextExecution(cronExpression: String?): Instant? {
        if (cronExpression.isNullOrEmpty()) return null

        val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
        val cron = parser.parse(cronExpression)
        cron.validate() // Ensures the cron expression is valid

        // Compute the next execution time
        val executionTime = ExecutionTime.forCron(cron)
        val now = ZonedDateTime.now()

        return executionTime.nextExecution(now).getOrNull()?.toInstant()
    }
}