package gov.cdc.ocio.eventreadersink.sink

import gov.cdc.ocio.eventreadersink.exceptions.BadStateException
import gov.cdc.ocio.eventreadersink.model.CloudConfig
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The EventProcessor class is responsible for processing events
 * by utilizing the provided CloudConfig. It integrates with the
 * CamelProcessor to sink messages into storage.
 *
 * @property cloudConfig An instance of CloudConfig that contains
 *                        configuration details for cloud processing.
 */
class EventProcessor(private val cloudConfig: CloudConfig) : KoinComponent {
    private val camelProcessor: CamelProcessor by inject()
    private val logger = KotlinLogging.logger {}

    /**
     * Processes an event by invoking the sinkMessageToStorage method
     * of the CamelProcessor with the provided CloudConfig.
     *
     * This function handles any exceptions that may occur during
     * the processing, logging an error message if an exception is
     * thrown.
     */
    @Throws(BadStateException::class, IllegalArgumentException::class)
    fun processEvent() {
        try {
            camelProcessor.sinkMessageToStorage(cloudConfig)
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to process event due to invalid argument: ${e.message}", e)
            throw BadStateException("Invalid argument provided: ${e.message}")
        } catch (e: BadStateException) {
            logger.error("Failed to process event: ${e.message}")
            throw e // Re-throwing the original exception
        } catch (e: Exception) {
            logger.error("Failed to process event due to an unexpected error: ${e.message}", e)
            throw BadStateException("Unexpected error occurred  ${e.message}")
        }
    }
}