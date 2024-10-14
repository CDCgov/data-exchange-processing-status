package gov.cdc.ocio.rulesengine.exception

/**
 * The main exception which gets thrown during the CRUD operations
 */
class RulesEngineException(message: String, cause: Throwable? = null)  : RuntimeException(message, cause)
