package gov.cdc.ocio.eventreadersink.exceptions

/**
 * Intended use of this exception is for bad requests, or, if a required parameter is missing for a request.
 *
 * @constructor Creates a new instance of [BadServiceException] with the specified error message.
 * @param message The detail message explaining the cause of the exception.
 */
class BadServiceException(message: String): Exception(message)