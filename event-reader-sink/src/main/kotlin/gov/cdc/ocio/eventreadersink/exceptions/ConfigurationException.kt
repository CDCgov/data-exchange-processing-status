package gov.cdc.ocio.eventreadersink.exceptions

/**
 * Exception thrown for general configuration loading errors.
 *  * @constructor Creates a new instance of [ConfigurationException] with the specified error message.
 *  * @param message The detail message explaining the cause of the exception.
 */
class ConfigurationException(message: String) : Exception(message)