package gov.cdc.ocio.subscriptionmanagement.interfaces

import gov.cdc.ocio.subscriptionmanagement.exception.SubscriptionManagementException

/** The core RulesEngine interface for managing rules (CRUD operations).
 *
 */

interface SubscriptionManagementEngine {

    @Throws(SubscriptionManagementException::class)
    fun subscribe(subscription: WorkflowSubscription): String? // Returns the rule ID

    @Throws(SubscriptionManagementException::class)
    fun unsubscribe(subscriptionId: String, notificationId:String): Boolean // True if successful, false otherwise

    @Throws(SubscriptionManagementException::class)
    fun updateSubscription(updatedSubscription: WorkflowSubscription): WorkflowSubscription?

    @Throws(SubscriptionManagementException::class)
    fun getSubscriptionById(subscriptionId: String): WorkflowSubscription?

    @Throws(SubscriptionManagementException::class)
    fun getSubscriptions(): List<WorkflowSubscription?>

    @Throws(SubscriptionManagementException::class)
    fun evaluateSubscription(subscriptionId: String, data: Map<String, Any>): Boolean // Evaluate subscriptions against some data
}
