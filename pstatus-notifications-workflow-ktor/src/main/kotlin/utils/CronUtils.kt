package gov.cdc.ocio.processingnotifications.utils

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import java.util.*

object CronUtils {

    /**
     * Returns a human-readable version of the provided cron schedule (UNIX format).
     *
     * @param cronSchedule String
     * @return String
     */
    fun getCronScheduleDescription(cronSchedule: String): String {
        // Get a predefined instance
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)

        // Parse cronSchedule expression and get description
        val parser = CronParser(cronDefinition)
        val descriptor = CronDescriptor.instance(Locale.US)
        return descriptor.describe(parser.parse(cronSchedule))
    }
}