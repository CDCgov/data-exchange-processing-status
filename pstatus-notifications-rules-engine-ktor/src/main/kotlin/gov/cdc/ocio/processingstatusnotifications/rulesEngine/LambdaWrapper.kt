package gov.cdc.ocio.processingstatusnotifications.rulesEngine

/**
 * Wrapper to facilitate calling actions with a lambda expression.
 *
 * @property action Function3<String, String, String, Unit>
 * @constructor
 */
class LambdaWrapper(val action: (String, String, String) -> Unit) {
    /**
     * Wrapper to call the action with the provided parameters.
     *
     * @param subscriptionId String
     * @param ruleConditionBase64Encoded String
     * @param reportJsonBase64Encoded String
     */
    fun call(
        subscriptionId: String,
        ruleConditionBase64Encoded: String,
        reportJsonBase64Encoded: String
    ) = action(subscriptionId, ruleConditionBase64Encoded, reportJsonBase64Encoded)
}