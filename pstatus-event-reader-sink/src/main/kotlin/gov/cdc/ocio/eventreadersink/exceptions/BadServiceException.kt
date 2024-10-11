package gov.cdc.ocio.eventreadersink.exceptions

/**
 * Intended use of this exception is for bad requests, or, if a required parameter is missing for a request.
 *
 * @constructor
 */
class BadServiceException(message: String): Exception(message)