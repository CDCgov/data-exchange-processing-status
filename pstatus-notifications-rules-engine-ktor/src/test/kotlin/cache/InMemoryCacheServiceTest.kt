package cache

import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionType
import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService
import gov.cdc.ocio.processingstatusnotifications.exception.BadStateException
import gov.cdc.ocio.processingstatusnotifications.model.message.Status
import org.testng.Assert
import org.testng.annotations.Test


class InMemoryCacheServiceTest {

    private var inMemoryCacheService: InMemoryCacheService = InMemoryCacheService()

    @Test(description = "This test asserts true for generating two unique subscriptionIds for same user")
    fun testAddingSameNotificationPreferencesSuccess() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "service1","action1", Status.FAILURE,
            "abc@trh.com", SubscriptionType.EMAIL
        )
        val subscriptionId2 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "service1","action1", Status.FAILURE,
            "rty@trh.com", SubscriptionType.EMAIL
        )
        Assert.assertEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test asserts true for generating two unique subscriptionIds for different set of rules for same user")
    fun testAddingDifferentNotificationPreferencesSuccess() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "service1","action1", Status.FAILURE,
            "abc@trh.com", SubscriptionType.EMAIL
        )
        val subscriptionId2 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "service1","action1", Status.SUCCESS,
            "abc@trh.com", SubscriptionType.EMAIL
        )
        Assert.assertNotEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test asserts true for unsubscribing existing susbcription")
    fun testUnsubscribingSubscriptionSuccess() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "service1","action1", Status.FAILURE,
            "abc@trh.com", SubscriptionType.EMAIL
        )

        Assert.assertTrue(inMemoryCacheService.unsubscribeNotifications(subscriptionId1))
    }

    @Test(description = "This test throws exception for unsubscribing susbcriptionId that doesn't exist")
    fun testUnsubscribingSubscriptionException() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "service1","action1", Status.FAILURE,
            "abc@trh.com", SubscriptionType.EMAIL
        )
        // Remove subscription first
        inMemoryCacheService.unsubscribeNotifications(subscriptionId1)

        try {
            inMemoryCacheService.unsubscribeNotifications(subscriptionId1)
        } catch (e: BadStateException) {
            Assert.assertEquals(e.message, "Subscription doesn't exist")
        }
    }
}

