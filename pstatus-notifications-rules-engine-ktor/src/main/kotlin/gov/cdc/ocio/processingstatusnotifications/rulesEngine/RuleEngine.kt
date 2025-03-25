package gov.cdc.ocio.processingstatusnotifications.rulesEngine

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusnotifications.model.EmailNotification
import gov.cdc.ocio.processingstatusnotifications.model.Subscription
import gov.cdc.ocio.processingstatusnotifications.model.SubscriptionRule
import gov.cdc.ocio.processingstatusnotifications.model.WebhookNotification
import gov.cdc.ocio.processingstatusnotifications.model.report.ReportMessage
import gov.cdc.ocio.processingstatusnotifications.model.report.Status
import gov.cdc.ocio.processingstatusnotifications.utils.ObjectMapper
import gov.cdc.ocio.types.adapters.DateLongFormatTypeAdapter
import gov.cdc.ocio.types.adapters.InstantTypeAdapter
import io.ktor.util.*
import mu.KotlinLogging
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.mvel.MVELRule
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*


/**
 * Manages the rules engine
 */
object RuleEngine: KoinComponent {

    private val logger = KotlinLogging.logger {}

//    private val repository by inject<ProcessingStatusRepository>()

    private val rulesEngine = DefaultRulesEngine()

    private val gson by lazy {
        GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

//    private val notificationSubscriptions = repository.notificationSubscriptionsCollection

    private val subscriptions = mapOf(
        UUID.randomUUID().toString() to Subscription(
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
        UUID.randomUUID().toString() to Subscription(
            subscriptionRule = SubscriptionRule(
                dataStreamId = "dex-testing1",
                dataStreamRoute = "test-event1",
                jurisdiction = null,
                mvelRuleCondition = "stageInfo.service == 'UPLOAD API' && stageInfo.action == 'upload-completed' && stageInfo.status == Status.SUCCESS"
            ),
            notification = WebhookNotification(
                webhookUrl = "https://webhook.site/8267e7c4-48a0-4f1d-8005-3c43a039d7e0"
            )
        )
    )

    /**
     * Evaluate all subscriptions to see if a notification needs to be sent.
     *
     * @param report ReportMessage
     */
    fun evaluateAllRules(
        report: ReportMessage
    ) {
        for (subscription in subscriptions) {
            evaluateSubscription(report, subscription)
        }
    }

    /**
     * Evaluate the provided subscription to determine whether the conditions for the subscription have been met and
     * if so, send the notification.
     *
     * @param report ReportMessage
     * @param subscriptionEntry Entry<String, Subscription>
     */
    private fun evaluateSubscription(
        report: ReportMessage,
        subscriptionEntry: Map.Entry<String, Subscription>
    ) {
        val subscriptionId = subscriptionEntry.key
        val subscription = subscriptionEntry.value

        // Create facts
        val facts = Facts()
        val map = ObjectMapper.anyToMap(report)
        map.forEach {
            facts.put(it.key, it.value)
        }

        facts.put("ruleAction", ruleActionLambda())

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
            .then("ruleAction.call('$subscriptionId', '${condition.encodeBase64()}', '${reportMsgJson.encodeBase64()}');")

        val rules = Rules(rule)
        rules.register(rule)

        rulesEngine.fire(rules, facts)
    }

    /**
     * Generate the rule action lambda wrapper.
     *
     * @return LambdaWrapper
     */
    private fun ruleActionLambda() = LambdaWrapper { subscriptionId, conditionBase64Encoded, reportJsonBase64Encoded ->
        ruleActionFunction(subscriptionId, conditionBase64Encoded, reportJsonBase64Encoded)
    }

    /**
     * The rule action function is what is invoked by the rules engine whenever the conditions for the rule are met.
     *
     * @param subscriptionId String
     * @param ruleConditionBase64Encoded String
     * @param reportJsonBase64Encoded String
     */
    private fun ruleActionFunction(
        subscriptionId: String,
        ruleConditionBase64Encoded: String,
        reportJsonBase64Encoded: String
    ) {
        val ruleCondition = ruleConditionBase64Encoded.decodeBase64String()
        logger.info { "Function ruleActionFunction() was called with subscription id: $subscriptionId, rule condition: $ruleCondition" }
        val subscription = subscriptions.entries.firstOrNull { it.key == subscriptionId }?.value
        val report = gson.fromJson(reportJsonBase64Encoded.decodeBase64String(), ReportMessage::class.java)
        subscription?.doNotify(report)
    }

}
