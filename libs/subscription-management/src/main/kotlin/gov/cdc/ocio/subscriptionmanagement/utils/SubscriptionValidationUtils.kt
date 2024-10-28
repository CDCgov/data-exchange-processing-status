package gov.cdc.ocio.subscriptionmanagement.utils

import gov.cdc.ocio.subscriptionmanagement.exception.SubscriptionValidationException
import gov.cdc.ocio.subscriptionmanagement.interfaces.WorkflowSubscription

/**
 * Utility class for validating subscriptions before they are processed by the SubscriptionEngine.
 */
object SubscriptionValidationUtils {

    /**
     * Validates that the rule is not null and contains all required fields.
     * @param subscription WorkflowSubscription The subscription to be validated.
     * @throws SubscriptionValidationException if the subscription is invalid.
     */
    fun validateSubscription(subscription: WorkflowSubscription) {

        if (subscription.notificationId.isBlank()) {
            throw SubscriptionValidationException("Rule ID cannot be blank.")
        }
        if (subscription.notification.isBlank()) {
            throw SubscriptionValidationException("Rule Name cannot be blank.")
        }
        if (subscription.conditions.isEmpty()) {
            throw SubscriptionValidationException("Rule must contain at least one condition.")
        }
        if (subscription.actions.isEmpty()) {
            throw SubscriptionValidationException("Rule must specify at least one action.")
        }
    }

    /**
     * Validates that the subscription's conditions are valid.
     * @param subscription WorkflowSubscription The subscription containing conditions to be validated.
     * @throws SubscriptionValidationException if any condition is invalid.
     */
    fun validateConditions(subscription: WorkflowSubscription) {
        subscription.conditions.forEach { (field, _) ->
            if (field.isBlank()) {
                throw SubscriptionValidationException("Condition field cannot be blank.")
            }
        }
    }

    /**
     * Validates that the actions associated with a subscriptions are valid.
     * @param subscription WorkflowRule The subscription containing actions to be validated.
     * @throws SubscriptionValidationException if any action is invalid.
     */
    fun validateActions(subscription: WorkflowSubscription) {
        subscription.actions.forEach {
            if (it.name.isBlank()) {
                throw SubscriptionValidationException("Action name cannot be blank.")
            }
            if (it.parameters.isEmpty()) {
                throw SubscriptionValidationException("Action must contain at least one parameter.")
            }
        }
    }

    /** ~~~~FOR FUTURE USE~~~~~
     * Validates that the data provided for subscription evaluation matches the subscription's expected fields.
     * @param data Map<String, Any> The data to be evaluated by the subscription.
     * @param subscription WorkflowSubscription The subscription that the data will be validated against.
     * @throws SubscriptionValidationException if the data is invalid.
     */
    fun validateDataAgainstSubscription(data: Map<String, Any>, rule: WorkflowSubscription) {
        rule.conditions.forEach { (field, _) ->
            if (!data.containsKey(field)) {
                throw SubscriptionValidationException("Data missing required field: $field")
            }
        }
    }
}
