package gov.cdc.ocio.reportschemavalidator.exceptions

/**
 * Intended use of this exception is for bad requests, such as a record could not be located because an invalid
 * identifier was provided.  Or, if a required parameter is missing for a request.
 *
 * @constructor
 */
class BadRequestException(message: String): Exception(message)