package gov.cdc.ocio.processingnotifications.model

/**
 * Base class for subscription
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param cronSchedule String
 * @param emailAddresses List<String>
 */
open class BaseSubscription(
    open val dataStreamId: String,
    open val dataStreamRoute: String,
    open val jurisdiction: String,
    open val cronSchedule: String,
    open val emailAddresses: List<String>
)