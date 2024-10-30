package gov.cdc.ocio.subscriptionmanagement.implementation

import gov.cdc.ocio.subscriptionmanagement.exception.SubscriptionNotFoundException
import gov.cdc.ocio.subscriptionmanagement.exception.SubscriptionManagementException
import gov.cdc.ocio.subscriptionmanagement.interfaces.SubscriptionManagementEngine
import gov.cdc.ocio.subscriptionmanagement.interfaces.WorkflowSubscription
import gov.cdc.ocio.subscriptionmanagement.repository.SubscriptionManagementRepository
import gov.cdc.ocio.subscriptionmanagement.utils.SubscriptionValidationUtils
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import mu.KLogger
import kotlin.jvm.Throws

/**
 * The default implementation class which gets exposed to the other apps and libs and which abstracts the
 * persistence db CRUD operations
 * This uses the persistence layer the CosmosRuleRepository to perform the CRUD
 */
class SubscriptionManagementEngineImpl(
    private val repository: SubscriptionManagementRepository,
    private val logger: KLogger
) : SubscriptionManagementEngine {

    /**
     * The function which adds a rule ot the Cosmos Db
     * @param subscription WorkflowSubscription
     * @return String - the subscriptionId
     */
    @Throws(SubscriptionManagementException::class)
    override fun subscribe(subscription: WorkflowSubscription): String? {
        return try {
            // Validate the rule before adding it to the system
            SubscriptionValidationUtils.validateSubscription(subscription)
            SubscriptionValidationUtils.validateConditions(subscription)
            SubscriptionValidationUtils.validateActions(subscription)
           val workflowSubscription = repository.saveSubscription(subscription)
            val subscriptionId = workflowSubscription.subscriptionId
            logger.info("Rule added with Id: $subscriptionId")
            subscriptionId

        } catch (ex: SubscriptionManagementException) {
            logger.error("Error adding rule: ${ex.message}")
            throw ex
        }
    }
    /**
     * The function which unsubscribes a subscription by subscriptionId and partition key notificationId
     * @param subscriptionId String
     * @param notificationId String
     * @return Boolean
     */
    @Throws(SubscriptionManagementException::class)
    override fun unsubscribe(subscriptionId: String, notificationId:String): Boolean {
        return try {
            val result = repository.deleteSubscription(subscriptionId, notificationId)
            logger.info("Subscription deleted: $subscriptionId")
            result

        } catch (ex: SubscriptionManagementException) {
            logger.error("Error deleting subscription: ${ex.message}")
            throw ex
        }
    }
    /**
     * The function which gets a subscription by subscriptionId
     * @param subscriptionId String
     * @return Boolean
     */
    @Throws(SubscriptionNotFoundException::class)
    override fun getSubscriptionById(subscriptionId: String): WorkflowSubscription? {
        return try {
            val result = repository.findSubscriptionById(subscriptionId)
            result
        } catch (ex: SubscriptionNotFoundException) {
            logger.error("Error retrieving subscription: ${ex.message}")
            throw ex
        }
    }
    /**
     * The function which gets all subscriptions
     * @return List<WorkflowSubscription>
     */
    @Throws(SubscriptionManagementException::class)
    override fun getSubscriptions(): List<WorkflowSubscription?> {
        return try {
            val results=  repository.findAllSubscriptions()
            results
        } catch (ex: SubscriptionManagementException) {
            logger.error("Error retrieving all subscriptions: ${ex.message}")
            throw ex
        }
    }
    override fun updateSubscription(updatedSubscription: WorkflowSubscription): WorkflowSubscription? {
        throw NotImplementedException("This function has not yet been implemented")
    }
    override fun evaluateSubscription(subscriptionId: String, data: Map<String, Any>): Boolean  {
        throw NotImplementedException("This function has not yet been implemented")
    }

}
