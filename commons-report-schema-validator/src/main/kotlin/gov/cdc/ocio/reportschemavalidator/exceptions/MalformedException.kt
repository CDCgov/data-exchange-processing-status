package gov.cdc.ocio.reportschemavalidator.gov.cdc.ocio.reportschemavalidator.exceptions

/**
 * Intended use of this exception is for bad requests, such as a malformed JSON or error processing the report
 * Note, this is needed when the
 * content may be malformed, such as the structure not being valid JSON or elements not of the expected type.
 *
 * @constructor
 */
class MalformedException(message: String): Exception(message)