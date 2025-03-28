@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.exceptions.ResponseException
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


/**
 * Email subscription data class.
 *
 * @property dataStreamId String
 * @property dataStreamRoute String
 * @property jurisdiction String?
 * @property mvelCondition String
 * @property emailAddresses List<String>
 * @constructor
 */
@Serializable
data class EmailSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String?,
    val mvelCondition: String,
    val emailAddresses: List<String>
)

/**
 * Webhook subscription data class.
 *
 * @property dataStreamId String
 * @property dataStreamRoute String
 * @property jurisdiction String?
 * @property mvelCondition String
 * @property webhookUrl String
 * @constructor
 */
@Serializable
data class WebhookSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String?,
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
 * response which contains the subscriberId, message and the status of subscribe/unsubscribe operations.
 *
 * @param subscriptionId
 * @param timestamp
 * @param status
 * @param message
 */
@Serializable
data class SubscriptionResult(
    var subscriptionId: String? = null,
    var timestamp: Long? = null,
    var status: Boolean? = false,
    var message: String? = ""
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
        mvelCondition: String,
        emailAddresses: List<String>
    ): SubscriptionResult {
        val url = rulesEngineServiceConnection.buildUrl("/subscribe/email")

        return runBlocking {
            val result = runCatching {
                val response = rulesEngineServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(EmailSubscription(dataStreamId, dataStreamRoute, jurisdiction, mvelCondition, emailAddresses))
                }
                return@runCatching processResponse(response)
            }
            result.onFailure {
                when (it) {
                    is ResponseException -> throw it
                    else -> throw Exception(rulesEngineServiceConnection.serviceUnavailable)
                }
            }
            return@runBlocking result.getOrThrow()
        }
    }

    /**
     * SubscribeWebhook function which in turn uses the http client to invoke the notifications ktor microservice
     * route to subscribe webhook notifications.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String?
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
        mvelCondition: String,
        webhookUrl: String
    ): SubscriptionResult {
        val url = rulesEngineServiceConnection.buildUrl("/subscribe/webhook")

        return runBlocking {
            try {
                val response = rulesEngineServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(WebhookSubscription(dataStreamId, dataStreamRoute, jurisdiction, mvelCondition, webhookUrl))
                }
                return@runBlocking processResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    SubscriptionResponse.ProcessErrorCodes(url, e, null)
                }
                throw Exception(rulesEngineServiceConnection.serviceUnavailable)
            }
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
            try {
                val response = rulesEngineServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(UnsubscribeRequest(subscriptionId))
                }
                return@runBlocking processResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    SubscriptionResponse.ProcessErrorCodes(url, e, subscriptionId)
                }
                throw Exception(rulesEngineServiceConnection.serviceUnavailable)
            }
        }
    }

    companion object {

        /**
         * Function to process the http response coming from notifications service
         * @param response HttpResponse
         */
        private suspend fun processResponse(response: HttpResponse): SubscriptionResult {
            if (response.status == HttpStatusCode.OK) {
                return response.body()
            } else {
                throw Exception("Notification rules engine service is unavailable. Status: ${response.status}")
            }
        }
    }

}
