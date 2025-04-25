package gov.cdc.ocio.processingstatusapi.processors

import gov.cdc.ocio.messagesystem.models.Source


/**
 * Processor for handling messages received from RabbitMQ.
 * Inherits `MessageProcessor` which defines core functionality for message processing.
 *
 * @property source indicates the source of the message as `RABBITMQ`, is included in the persisted report,
 *                  for origin tracking.
 */
class RabbitMQProcessor: MessageProcessor() {
    override val source: Source = Source.RABBITMQ
}