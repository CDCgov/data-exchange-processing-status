package gov.cdc.ocio.cache

import gov.cdc.ocio.exceptions.BadStateException
import gov.cdc.ocio.model.cache.SubscriptionRule
import gov.cdc.ocio.model.http.SubscriptionType


class InMemoryCacheService {

    fun updateNotificationsPreferences(destinationId: String,
                                                    eventType: String,
                                                    stageName: String,
                                                    statusType: String,
                                                    emailOrUrl: String,
                                                    subscriptionType: SubscriptionType
    ): String{
        try {
            val subscriptionRule = SubscriptionRule(destinationId, eventType, stageName, statusType)
            val subscriptionId =  InMemoryCache.updateCacheForSubscription(subscriptionRule.getStringHash(), subscriptionType, emailOrUrl)
            return subscriptionId
        } catch (e: BadStateException) {
            throw e
        }

    }

    fun unsubscribeNotifications(subscriptionId: String): Boolean {
        try {
            return InMemoryCache.unsubscribeSubscriber(subscriptionId)
        } catch (e: BadStateException) {
            throw e
        }

    }
}