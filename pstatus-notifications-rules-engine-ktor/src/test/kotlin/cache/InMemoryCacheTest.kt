package cache


import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCache
import gov.cdc.ocio.processingstatusnotifications.exception.BadStateException
import org.testng.Assert
import org.testng.annotations.Test

class InMemoryCacheTest {

    private var inMemoryCache: InMemoryCache = InMemoryCache

    @Test(description = "This test asserts true for generating two unique subscriptionId")
    fun testTwoUniqueSubscriptionIdGenerated() {
        val subscriptionId1 = inMemoryCache.generateUniqueSubscriptionId()
        val subscriptionId2 = inMemoryCache.generateUniqueSubscriptionId()
        Assert.assertNotEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test assert true for generating two unique subscriptionId for two different rules.")
    fun testSuccessTwoNewRules() {
        val subscriptionRule1 = "subscriptionRuleUnique1"
        val subscriptionRule2 = "subscriptionRuleUnique2"

        val subscriptionId1 = inMemoryCache.updateCacheForSubscription(subscriptionRule1, SubscriptionType.EMAIL, "trr@ddf.ccc")
        val subscriptionId2 = inMemoryCache.updateCacheForSubscription(subscriptionRule2, SubscriptionType.EMAIL, "trr@ddf.ccc")
        Assert.assertNotEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test assert true for generating one unique subscriptionId for single different rules for two users")
    fun testSuccessTwoSubscribersSingleRules() {
        val subscriptionRule1 = "subscriptionRuleUnique1"

        val subscriptionId1 = inMemoryCache.updateCacheForSubscription(subscriptionRule1, SubscriptionType.EMAIL, "trr@ddf.ccc")
        val subscriptionId2 = inMemoryCache.updateCacheForSubscription(subscriptionRule1, SubscriptionType.WEBHOOK, "tre@ddf.ccc")
        Assert.assertEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test assert true for unsubscribing existing subscription which exist")
    fun testSuccessUnsubscribeExistingSubscription() {
        val subscriptionRule1 = "subscriptionRuleUnique1"
        val subscriptionId1 = inMemoryCache.updateCacheForSubscription(subscriptionRule1, SubscriptionType.EMAIL, "trr@ddf.ccc")

        Assert.assertTrue(inMemoryCache.unsubscribeSubscriber(subscriptionId1))
    }

    @Test(description = "This test assert false for unsubscribing subscription which doesn't exist")
    fun testFailureUnsubscribeSubscription() {
        val subscriptionRule1 = "subscriptionRuleUnique1"
        val subscriptionId1 = inMemoryCache.updateCacheForSubscription(subscriptionRule1, SubscriptionType.EMAIL, "trr@ddf.ccc")
        var exceptionThrown = false
        // Delete once so the subscription for this user doesn't exist
        inMemoryCache.unsubscribeSubscriber(subscriptionId1)
        try {
            inMemoryCache.unsubscribeSubscriber(subscriptionId1)
        } catch(ex: BadStateException) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)

    }
}