package gov.cdc.ocio.processingstatusnotifications.exception

/**
 * Intended use of this exception is for internal server issues where we expect to be in a certain state or have
 * internal state information that is missing or invalid.
 *
 * @constructor
 */
class BadStateException(message: String): Exception(message)