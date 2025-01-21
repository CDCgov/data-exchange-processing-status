package gov.cdc.ocio.processingstatusapi.plugins

import gov.cdc.ocio.processingstatusapi.models.Source


/**
 * Processor for handling messages received from Azure Service Bus Queue or Topic.
 * Inherits `MessageProcessor` which defines core functionality for message processing.
 *
 * @property source indicates the source of the message as `SERVICEBUS`, is included in the persisted report,
 * for origin tracking.
 */
class ServiceBusProcessor: MessageProcessor() {
    override val source: Source = Source.SERVICEBUS
}