package gov.cdc.ocio.processingstatusnotifications.rulesEngine


import gov.cdc.ocio.processingstatusnotifications.model.cache.NotificationSubscription
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService

interface Rule {
    fun evaluateAndDispatch(ruleId: String, cacheService: InMemoryCacheService): String
    fun dispatchEvent(subscription: NotificationSubscription): String
}