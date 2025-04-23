@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.net.ConnectException


/**
 * Email subscription data class.
 *
 * @property dataStreamId String
 * @property dataStreamRoute String
 * @property jurisdiction String?
 * @property ruleDescription String?
 * @property mvelCondition String
 * @property emailAddresses List<String>
 * @constructor
 */
@Serializable
data class EmailSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String?,
    val ruleDescription: String?,
    val mvelCondition: String,
    val emailAddresses: List<String>
)

/**
 * Webhook subscription data class.
 *
 * @property dataStreamId String
 * @property dataStreamRoute String
 * @property jurisdiction String?
 * @property ruleDescription String?
 * @property mvelCondition String
 * @property webhookUrl String
 * @constructor
 */
@Serializable
data class WebhookSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String?,
    val ruleDescription: String?,
    val mvelCondition: String,
    val webhookUrl: String
)

/**
 * Unsubscribe request, which only requires the subscription id to unsubscribe.
 *
 * @param subscriptionId
 */
@Serializable
data class UnsubscribeRequest(val subscriptionId: String)

/**
 * SubscriptionResult is the response class which is serialized back and forth which is in turn used for getting the
 * response.
 *
 * @param subscriptionId
 */
@Serializable
data class SubscriptionResult(
    var subscriptionId: String? = null
)

/**
 * The graphQL mutation class for notifications
 */
class NotificationsRulesEngineMutationService(
    rulesEngineServiceUrl: String?
) : Mutation {

    private val rulesEngineServiceConnection =
        ServiceConnection("notifications rules engine", rulesEngineServiceUrl)

    /**
     * SubscribeEmail function which in turn uses the http client to invoke the notifications ktor microservice
     * route to subscribe.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String?
     * @param ruleDescription String?
     * @param mvelCondition String
     * @param emailAddresses List<String>
     * @return SubscriptionResult
     */
    @GraphQLDescription("Subscribe Email Notifications")
    @Suppress("unused")
    fun subscribeEmail(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String?,
        ruleDescription: String?,
        mvelCondition: String,
        emailAddresses: List<String>
    ): SubscriptionResult {
        val url = rulesEngineServiceConnection.buildUrl("/subscribe/email")

        return runBlocking {
            val response = runCatching {
                rulesEngineServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(EmailSubscription(
                        dataStreamId,
                        dataStreamRoute,
                        jurisdiction,
                        ruleDescription,
                        mvelCondition,
                        emailAddresses)
                    )
                }
            }.onFailure {
                if (it is ConnectException)
                    throw ConnectException(rulesEngineServiceConnection.serviceUnavailable)
            }.getOrThrow()
            return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
        }
    }

    /**
     * SubscribeWebhook function which in turn uses the http client to invoke the notifications ktor microservice
     * route to subscribe webhook notifications.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String?
     * @param ruleDescription String?
     * @param mvelCondition String
     * @param webhookUrl String
     * @return SubscriptionResult
     */
    @GraphQLDescription("Subscribe Webhook Notifications")
    @Suppress("unused")
    fun subscribeWebhook(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String?,
        ruleDescription: String?,
        mvelCondition: String,
        webhookUrl: String
    ): SubscriptionResult {
        val url = rulesEngineServiceConnection.buildUrl("/subscribe/webhook")

        return runBlocking {
            val response = runCatching {
                rulesEngineServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(WebhookSubscription(
                        dataStreamId,
                        dataStreamRoute,
                        jurisdiction,
                        ruleDescription,
                        mvelCondition,
                        webhookUrl)
                    )
                }
            }.onFailure {
                if (it is ConnectException)
                    throw ConnectException(rulesEngineServiceConnection.serviceUnavailable)
            }.getOrThrow()
            return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
        }
    }

    /**
     * UnSubscribeWebhook function which in turn uses the http client to invoke the notifications ktor microservice
     * route to unsubscribe webhook notifications.
     *
     * @param subscriptionId String
     */
    @GraphQLDescription("Unsubscribe Notifications")
    @Suppress("unused")
    fun unsubscribe(subscriptionId: String): SubscriptionResult {
        val url = rulesEngineServiceConnection.buildUrl("/unsubscribe")

        return runBlocking {
            val response = runCatching {
                rulesEngineServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(UnsubscribeRequest(subscriptionId))
                }
            }.onFailure {
                if (it is ConnectException)
                    throw ConnectException(rulesEngineServiceConnection.serviceUnavailable)
            }.getOrThrow()
            return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
        }
    }

}
