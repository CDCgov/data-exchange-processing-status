package gov.cdc.ocio.types.model


/**
 * Subscription rule definition, which defines the conditions under which the notification will be triggered.
 *
 * @property dataStreamId [String] needed for security
 * @property dataStreamRoute [String] needed for security
 * @property jurisdiction [String]? possibly needed for security
 * @property mvelRuleCondition [String] MVFLEX Expression Language (MVEL) specifying the condition the rule is triggered
 * resulting in a notification.
 * @constructor
 */
data class SubscriptionRule(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String?,
    val mvelRuleCondition: String
)
