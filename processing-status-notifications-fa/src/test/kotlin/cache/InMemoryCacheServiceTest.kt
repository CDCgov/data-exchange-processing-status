package cache

import gov.cdc.ocio.cache.InMemoryCacheService
import gov.cdc.ocio.exception.BadStateException
import gov.cdc.ocio.model.http.SubscriptionType
import org.testng.Assert.*
import org.testng.annotations.Test

class InMemoryCacheServiceTest {

    private var inMemoryCacheService: InMemoryCacheService = InMemoryCacheService()

    @Test(description = "This test asserts true for generating two similar subscriptionIds for same set of rules for two different users")
    fun testAddingSameNotificationPreferencesSuccess() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "stageName1","warning",
            "abc@trh.com", SubscriptionType.EMAIL
        )
        val subscriptionId2 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "stageName1","warning",
            "rty@trh.com", SubscriptionType.EMAIL
        )
        assertEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test asserts true for generating two unique subscriptionIds for different set of rules for same user")
    fun testAddingDifferentNotificationPreferencesSuccess() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "stageName1","warning",
            "abc@trh.com", SubscriptionType.EMAIL
        )
        val subscriptionId2 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "stageName1","success",
            "abc@trh.com", SubscriptionType.EMAIL
        )
        assertNotEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test asserts true for unsubscribing existing susbcription")
    fun testUnsubscribingSubscriptionSuccess() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "stageName1","warning",
            "abc@trh.com", SubscriptionType.EMAIL
        )

        assertTrue(inMemoryCacheService.unsubscribeNotifications(subscriptionId1))
    }

    @Test(description = "This test throws exception for unsubscribing susbcriptionId that doesn't exist")
    fun testUnsubscribingSubscriptionException() {
        val subscriptionId1 = inMemoryCacheService.updateNotificationsPreferences(
            "destination1","dataStreamRoute1",
            "stageName1","warning",
            "abc@trh.com", SubscriptionType.EMAIL
        )
        // Remove subscription first
        inMemoryCacheService.unsubscribeNotifications(subscriptionId1)

        try {
            inMemoryCacheService.unsubscribeNotifications(subscriptionId1)
        } catch (e: BadStateException) {
           assertEquals(e.message, "Subscription doesn't exist")
        }
    }
}

