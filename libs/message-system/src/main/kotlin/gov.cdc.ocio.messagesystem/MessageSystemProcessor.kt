package gov.cdc.ocio.messagesystem

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext

/**
 * The interface which when implemented will decide how to process the incoming message
 */
interface MessageSystemProcessor {
    fun processMessage(context: ServiceBusReceivedMessageContext)
}