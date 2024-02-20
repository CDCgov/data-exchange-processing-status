package gov.cdc.ocio.rulesEngine

import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.model.cache.NotificationSubscription


interface Rule {
    fun evaluate(ruleId: String, cacheService: InMemoryCacheService): Boolean

    fun dispatchEvent(subscription: NotificationSubscription)
}