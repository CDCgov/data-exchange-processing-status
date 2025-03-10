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
     * Validates the provided cron expression and throws an IllegalArgumentException if it is invalid.  Note, the
     * expected CRON type syntax is UNIX.
     *
     * @param cronExpression String?
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class)
    fun checkValid(cronExpression: String?) {
        if (cronExpression.isNullOrEmpty())
            throw IllegalArgumentException("Cron expression may not be null or empty")

        val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
        parser.parse(cronExpression) // throws IllegalArgumentException if invalid
    }

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
        return descriptor.describe(parser.parse(cronExpression))
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