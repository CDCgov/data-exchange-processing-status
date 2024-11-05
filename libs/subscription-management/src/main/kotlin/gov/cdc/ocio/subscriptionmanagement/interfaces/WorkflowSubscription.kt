package gov.cdc.ocio.subscriptionmanagement.interfaces

import gov.cdc.ocio.subscriptionmanagement.models.WorkflowActivity

/**
 * The interface which defines the work flow rule attributes
 *
 */
interface WorkflowSubscription {
    val subscriptionId: String? // primary key
    val notificationId:String //partition key
    val notification:String
    val conditions: Map<String, Any> // Condition map defining the rule logic
    val state: String // ***For future use*** - State associated with this rule, e.g., "ACTIVE", "INACTIVE"
    val actions: List<WorkflowActivity> // List of actions to be taken when conditions are met
    fun evaluate(data: Map<String, Any>): Boolean // ***For future use***  Logic for evaluating conditions
}
