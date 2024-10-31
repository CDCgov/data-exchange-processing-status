package gov.cdc.ocio.eventreadersink.camel

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

/* Appender for capturing logs during tests.
 * Used for verifying log output.
*/
class TestLogAppender : AppenderBase<ILoggingEvent>() {
    private val logMessages = mutableListOf<String>()

    override fun append(eventObject: ILoggingEvent) {
        logMessages.add(eventObject.formattedMessage)
    }

    fun getLogMessages(): List<String> = logMessages

    fun clear() {
        logMessages.clear()
    }
}
