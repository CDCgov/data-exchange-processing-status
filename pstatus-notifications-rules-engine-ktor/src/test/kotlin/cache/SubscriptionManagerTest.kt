package cache

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusnotifications.subscription.SubscriptionManager
import gov.cdc.ocio.processingstatusnotifications.exception.BadStateException
import gov.cdc.ocio.processingstatusnotifications.subscription.CachedSubscriptionLoader
import gov.cdc.ocio.processingstatusnotifications.subscription.DatabaseSubscriptionLoader
import gov.cdc.ocio.types.model.WebhookNotification
import io.mockk.every
import io.mockk.mockk
import org.testng.annotations.BeforeTest
import org.testng.annotations.AfterTest
import org.testng.annotations.Test
import org.testng.Assert
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module


class SubscriptionManagerTest {

    private val processingStatusRepoMock = mockk<ProcessingStatusRepository>()

    private lateinit var subscriptionManager: SubscriptionManager

    @BeforeTest
    fun setup() {
        startKoin {
            modules(
                module {
                    single { processingStatusRepoMock }
                    single { CachedSubscriptionLoader(DatabaseSubscriptionLoader()) }
                }
            )
        }
        every { processingStatusRepoMock.notificationSubscriptionsCollection } returns MockCollection()
        subscriptionManager = SubscriptionManager()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

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

