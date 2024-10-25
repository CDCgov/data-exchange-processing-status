package gov.cdc.ocio.subscriptionmanagement.interfaces

/**
 * The interface which defines the CRUD operations to perform on CosmosDB
 * using the CosmosRepository from commons-database library
 */

interface SubscriptionManagementRepository {

    /**
     * Create or update a subscription in the database.
     * @param subscription WorkflowSubscription The subscription to be persisted.
     * @return The persisted WorkflowSubscription.
     */
    fun saveSubscription(subscription: WorkflowSubscription): WorkflowSubscription

    /**
     * Fetch a subscription from the database by its ID.
     * @param subscriptionId String The ID of the rule.
     * @return The WorkflowSubscription if found, or null if not.
     */
    fun findSubscriptionById(subscriptionId: String): WorkflowSubscription?

    /**
     * Fetch all subscriptions from the database.
     * @return List<WorkflowRule> A list of all rules.
     */
    fun findAllSubscriptions(): List<WorkflowSubscription>

    /**
     * Delete a subscription by its ID.
     * @param subscriptionId String The ID of the subscription to delete.
     * @param notificationId String The ID of the notification to delete.
     * @return Boolean indicating whether the deletion was successful.
     */
    fun deleteSubscription(subscriptionId: String, notificationId:String): Boolean
}

