package gov.cdc.ocio.processingstatusnotifications.rulesEngine

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.processingstatusnotifications.model.EmailNotification
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import gov.cdc.ocio.processingstatusnotifications.model.cache.SubscriptionRule
import gov.cdc.ocio.processingstatusnotifications.model.WebhookNotification
import gov.cdc.ocio.processingstatusnotifications.model.message.ReportMessage
import gov.cdc.ocio.processingstatusnotifications.model.message.Status
import gov.cdc.ocio.types.adapters.DateLongFormatTypeAdapter
import gov.cdc.ocio.types.adapters.InstantTypeAdapter
import io.ktor.util.*
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.mvel.MVELRule
import java.time.Instant
import java.util.*


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

/**
 * Manages the rules engine
 */
object RuleEngine {

    private val rulesEngine = DefaultRulesEngine()

    private val gson: Gson by lazy {
        GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    private val subscriptions = listOf(
        Subscription(
            subscriptionRule = SubscriptionRule(
                dataStreamId = "dex-testing1",
                dataStreamRoute = "test-event1",
                jurisdiction = null,
                mvelRuleCondition = "stageInfo.service == 'UPLOAD API' && stageInfo.action == 'upload-status' && stageInfo.status != Status.SUCCESS"
            ),
            notification = EmailNotification(
                emailAddresses = listOf("ygj6@cdc.gov")
            )
        ),
        Subscription(
            subscriptionRule = SubscriptionRule(
                dataStreamId = "dex-testing1",
                dataStreamRoute = "test-event1",
                jurisdiction = null,
                mvelRuleCondition = "stageInfo.service == 'UPLOAD API' && stageInfo.action == 'upload-completed' && stageInfo.status == Status.SUCCESS"
            ),
            notification = WebhookNotification(
                webhookUrl = ""
            )

        )
    )

    fun evaluateAllRules(report: ReportMessage) {
        for (subscription in subscriptions) {
            evaluateSubscription(report, subscription)
        }
    }

    private fun evaluateSubscription(
        report: ReportMessage,
        subscription: Subscription
    ) {
        // Create facts
        val facts = Facts()
        val map = pojoToMap(report)
        map.forEach {
            facts.put(it.key, it.value)
        }

        facts.put("ruleAction",
            LambdaWrapper { subscriptionId, conditionBase64Encoded, reportJsonBase64Encoded ->
                ruleActionFunction(subscriptionId, conditionBase64Encoded, reportJsonBase64Encoded)
            }
        )

        // Pass the enum constants into the facts object
        facts.put("Status", Status::class.java)

        val context = subscription.subscriptionRule

        val condition = buildString {
            append("dataStreamId == '${context.dataStreamId}' && dataStreamRoute == '${context.dataStreamRoute}'")
            context.jurisdiction?.let {
                append(" && jurisdiction == '$it'")
            }
            append(" && ${context.mvelRuleCondition}")
        }

        val reportMsgJson = gson.toJson(report)

        // Fire rules
        val rule = MVELRule()
            .`when`(condition)
            .then("ruleAction.call('${subscription.subscriptionId}', '${condition.encodeBase64()}', '${reportMsgJson.encodeBase64()}');")

        val rules = Rules(rule)
        rules.register(rule)

        rulesEngine.fire(rules, facts)
    }

    private fun ruleActionFunction(subscriptionId: String, ruleConditionBase64Encoded: String, reportJsonBase64Encoded: String) {
        val ruleCondition = ruleConditionBase64Encoded.decodeBase64String()
        println("Function ruleActionFunction() was called with subscription id: $subscriptionId, rule condition: $ruleCondition")
        val subscription = subscriptions.first { it.subscriptionId == subscriptionId }
        val report = gson.fromJson(reportJsonBase64Encoded.decodeBase64String(), ReportMessage::class.java)
        subscription.doNotify(report)
    }

    private fun pojoToMap(pojo: Any): Map<String, Any?> {
        return pojo::class.java.declaredFields.associate { field ->
            field.isAccessible = true
            field.name to field.get(pojo)
        }
    }
}
