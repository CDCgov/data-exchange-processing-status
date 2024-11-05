package gov.cdc.ocio.subscriptionmanagement.repository

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.subscriptionmanagement.exception.SubscriptionNotFoundException
import gov.cdc.ocio.subscriptionmanagement.exception.SubscriptionManagementException
import gov.cdc.ocio.subscriptionmanagement.interfaces.SubscriptionManagementRepository
import gov.cdc.ocio.subscriptionmanagement.interfaces.WorkflowSubscription
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import java.util.*

/**
 * The class which implements the interface RulesRepository and uses the Cosmos Repository from commons-database lib
 * to persist rules, conditions, and actions.
 * @param repository ProcessingStatusRepository
 */
class SubscriptionManagementRepository(
   private val repository: ProcessingStatusRepository
) :  KoinComponent, SubscriptionManagementRepository {

    private val logger = KotlinLogging.logger {}
    /**
     * The function which saves the rule to the Cosmos SB container
     * @param subscription WorkflowSubscription
     * @return WorkflowRule
     */
    override fun saveSubscription(subscription: WorkflowSubscription): WorkflowSubscription {
        return try {
            val subscriptionId =  UUID.randomUUID().toString()
            repository.subscriptionManagementCollection.createItem(
                subscriptionId,
                subscription,
                WorkflowSubscription::class.java,
                subscription.notificationId)
            logger.info("Successfully saved rule with ID: ${subscription.notificationId}")
            subscription

        } catch (e: Exception) {
            logger.error("Failed to save subscription: ${subscription.notificationId}", e)
            throw SubscriptionManagementException("Failed to save the rule",e)
        }
    }
    /**
     * The function which retrieves a rule by ruleId
     * @param subscriptionId String
     * @return WorkflowRule
     */
    override fun findSubscriptionById(subscriptionId: String): WorkflowSubscription? {
        return try {
            val subscriptionManagementCollection = repository.subscriptionManagementCollection
            val cName = subscriptionManagementCollection.collectionNameForQuery
            val cVar = subscriptionManagementCollection.collectionVariable
            val cPrefix = subscriptionManagementCollection.collectionVariablePrefix
            val sqlQuery = (
                    "select * from $cName $cVar "
                            + "where ${cPrefix}ruleId = '$subscriptionId' ")
            val items = subscriptionManagementCollection.queryItems(
                sqlQuery,
                WorkflowSubscription::class.java
            )
            if(items.any()){
                items.first()
            }
            else
                null
        } catch (e: Exception) {
            logger.error("Subscription with ID $subscriptionId not found OR Error fetching rule with ID: $subscriptionId", e)
            throw SubscriptionNotFoundException("Subscription with id $subscriptionId not found in the container")

        }
    }
    /**
     * The function which retrieves all the rules under the Rules container
     * @return  List<WorkflowRule>
     */
    override fun findAllSubscriptions(): List<WorkflowSubscription> {
        try {
            val subscriptionManagementCollection = repository.subscriptionManagementCollection
            val cName = subscriptionManagementCollection.collectionNameForQuery
            val cVar = subscriptionManagementCollection.collectionVariable
            val sqlQuery = ("select * from $cName $cVar ")
            val items = subscriptionManagementCollection.queryItems(
                sqlQuery,
                WorkflowSubscription::class.java
            )
            return items
        } catch (e: Exception) {
            logger.error("Failed to fetch all subscriptions", e)
            throw SubscriptionManagementException("Failed to fetch all subscriptions from the subscription container", e)
        }
    }
    /**
     * The function which deletes a subscription by subscriptionId
     * @param subscriptionId String
     * @return  Boolean
     */
    override fun deleteSubscription(subscriptionId: String, notificationId:String): Boolean {
        return try {
            repository.subscriptionManagementCollection.deleteItem(
                subscriptionId,
                notificationId
            )
            true
        } catch (e: Exception ) {
            logger.error("Subscription with Id $subscriptionId not found for deletion or failed to delete subscription with ID: $subscriptionId", e)
            throw SubscriptionManagementException("Failed to delete the subscription using subscriptionId $subscriptionId", e)
        }
    }


}
