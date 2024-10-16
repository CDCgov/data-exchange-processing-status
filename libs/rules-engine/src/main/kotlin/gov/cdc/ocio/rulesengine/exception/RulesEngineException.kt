package gov.cdc.ocio.rulesengine.exception

/**
 * The default exception which gets thrown during the CRUD operations
 */
class RulesEngineException(message: String, cause: Throwable? = null)  : RuntimeException(message, cause)
