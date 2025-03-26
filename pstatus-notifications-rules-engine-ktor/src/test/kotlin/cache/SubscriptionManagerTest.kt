package cache

import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import gov.cdc.ocio.processingstatusnotifications.exception.BadStateException
import gov.cdc.ocio.types.model.WebhookNotification
import org.testng.Assert
import org.testng.annotations.Test


class SubscriptionManagerTest {

    private var subscriptionManager = SubscriptionManager()

    @Test(description = "This test asserts true for generating two unique subscriptionIds for same user")
    fun testAddingSameNotificationPreferencesSuccess() {
        val subscriptionId1 = subscriptionManager.upsertSubscription(
            "destination1","dataStreamRoute1",
            "jurisdiction1","stageInfo.status == Status.SUCCESS",
            WebhookNotification("http://somewebhook.com")
        )
        val subscriptionId2 = subscriptionManager.upsertSubscription(
            "destination1","dataStreamRoute1",
            "jurisdiction1","stageInfo.status == Status.SUCCESS",
            WebhookNotification("http://somewebhook.com")
        )
        Assert.assertEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test asserts true for generating two unique subscriptionIds for different set of rules for same user")
    fun testAddingDifferentNotificationPreferencesSuccess() {
        val subscriptionId1 = subscriptionManager.upsertSubscription(
            "destination1","dataStreamRoute1",
            "jurisdiction1","stageInfo.status == Status.SUCCESS",
            WebhookNotification("http://somewebhook.com")
        )
        val subscriptionId2 = subscriptionManager.upsertSubscription(
            "destination1","dataStreamRoute1",
            "jurisdiction1","stageInfo.status == Status.FAILURE",
            WebhookNotification("http://somewebhook.com")
        )
        Assert.assertNotEquals(subscriptionId1, subscriptionId2)
    }

    @Test(description = "This test asserts true for unsubscribing existing susbcription")
    fun testUnsubscribingSubscriptionSuccess() {
        val subscriptionId = subscriptionManager.upsertSubscription(
            "destination2","dataStreamRoute2",
            "jurisdiction2","stageInfo.status == Status.SUCCESS",
            WebhookNotification("http://somewebhook.com")
        )

        try {
            subscriptionManager.unsubscribeNotifications(subscriptionId)
        } catch (e: Exception) {
            Assert.fail("Expected no exception, but got: ${e.message}")
        }
    }

    @Test(
        description = "This test throws exception for unsubscribing to a subscription id that doesn't exist",
        expectedExceptions = [BadStateException::class]
    )
    fun testUnsubscribingSubscriptionException() {
        val subscriptionId = subscriptionManager.upsertSubscription(
            "destination2","dataStreamRoute2",
            "jurisdiction2","stageInfo.status == Status.SUCCESS",
            WebhookNotification("http://somewebhook.com")
        )
        // Remove subscription first
        subscriptionManager.unsubscribeNotifications(subscriptionId)

        // Calling a second time should cause an exception
        subscriptionManager.unsubscribeNotifications(subscriptionId)
    }
}

