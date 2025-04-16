package gov.cdc.ocio.processingnotifications.dispatch

import mu.KotlinLogging

class LogDispatcher : Dispatcher() {
    private val logger = KotlinLogging.logger {}

    override fun dispatch(data: Any) {
        logger.info("dispatching notification $data")
    }
}