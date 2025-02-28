package gov.cdc.ocio.messagesystem

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext

interface MessageSystemProcessor {
    fun processMessage(context: ServiceBusReceivedMessageContext)
}