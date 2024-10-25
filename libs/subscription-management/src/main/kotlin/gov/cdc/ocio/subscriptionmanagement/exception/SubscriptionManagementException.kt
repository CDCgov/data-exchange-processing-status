package gov.cdc.ocio.subscriptionmanagement.exception

/**
 * The default exception which gets thrown during the CRUD operations
 */
class SubscriptionManagementException(message: String, cause: Throwable? = null)  : RuntimeException(message, cause)
