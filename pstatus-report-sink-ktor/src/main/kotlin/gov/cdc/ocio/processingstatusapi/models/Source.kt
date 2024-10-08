package gov.cdc.ocio.processingstatusapi.models

/**
 * Indicates the source the data originates from.
 */
enum class Source {
    HTTP,
    SERVICEBUS,
    AWS,
    RABBITMQ
}