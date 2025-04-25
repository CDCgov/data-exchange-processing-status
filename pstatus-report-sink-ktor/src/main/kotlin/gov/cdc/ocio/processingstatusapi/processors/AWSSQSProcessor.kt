package gov.cdc.ocio.processingstatusapi.processors

import gov.cdc.ocio.messagesystem.models.Source


/**
 * Processor for handling messages received from AWS SQS.
 * Inherits `MessageProcessor` which defines core functionality for message processing.
 *
 * @property source indicates the source of the message as `AWS`, is included in the persisted report,
 *                  for origin tracking.
 */
class AWSSQSProcessor : MessageProcessor() {
    override val source: Source = Source.AWS
}