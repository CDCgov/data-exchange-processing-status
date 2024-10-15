package gov.cdc.ocio.eventreadersink.exceptions

/**
 * Intended use of this exception is for internal server issues where we expect to be in a certain state or have
 * internal state information that is missing or invalid.
 *
 * @constructor Creates a new instance of [BadStateException] with the specified error message.
 * @param message The detail message explaining the cause of the exception.
 */
class BadStateException(message: String): Exception(message)