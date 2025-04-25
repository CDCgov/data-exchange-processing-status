package gov.cdc.ocio.messagesystem.models

/**
 * Indicates the source the data originates from.
 */
enum class Source {
    HTTP,
    SERVICEBUS,
    AWS,
    RABBITMQ
}