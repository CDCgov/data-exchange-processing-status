package gov.cdc.ocio.rulesEngine

import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.cache.NotificationSubscription

interface Rule {
    fun evaluateAndDispatch(ruleId: String, cacheService: InMemoryCacheService): String
    fun dispatchEvent(subscription: NotificationSubscription): String
}