package gov.cdc.ocio.eventreadersink.exceptions

/**
 * Exception thrown when a required configuration property is missing.
 * @constructor Creates a new instance of [MissingPropertyException] with the specified error message.
 * @param message The detail message explaining the cause of the exception.
 */
class MissingPropertyException(message: String) : Exception(message)
