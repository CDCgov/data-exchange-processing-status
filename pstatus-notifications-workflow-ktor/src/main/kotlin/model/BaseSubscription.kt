package gov.cdc.ocio.processingnotifications.model

/**
 * Base class for subscription
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param daysToRun List<String>
 * @param timeToRun String
 * @param deliveryReference String
 */
open class BaseSubscription(open val dataStreamId: String,
                            open val dataStreamRoute: String,
                            open val jurisdiction: String,
                            open val daysToRun: List<String>,
                            open val timeToRun: String,
                            open val deliveryReference: String)