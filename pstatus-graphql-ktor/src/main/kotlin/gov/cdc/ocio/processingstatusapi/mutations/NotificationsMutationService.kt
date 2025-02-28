@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


/**
 * Email Subscription data class.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param email String
 * @param stageName String
 * @param statusType String
 */
@Serializable
data class EmailSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val email: String,
    val stageName: String,
    val statusType: String
)

/**
 * UnSubscription data class which is serialized back and forth which is in turn used for unsubscribing from the
 * cache for emails and webhooks using the given subscriberId.
 *
 * @param subscriptionId
 */
@Serializable
data class UnSubscription(val subscriptionId: String)

/**
 * SubscriptionResult is the response class which is serialized back and forth which is in turn used for getting the
 * response which contains the subscriberId, message and the status of subscribe/unsubscribe operations.
 *
 * @param subscription_id
 * @param timestamp
 * @param status
 * @param message
 */
@Serializable
data class SubscriptionResult(
    var subscription_id: String? = null,
    var timestamp: Long? = null,
    var status: Boolean? = false,
    var message: String? = ""
)

/**
 * The graphQL mutation class for notifications
 */
class NotificationsMutationService(
    workflowServiceUrl: String?
) : Mutation {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * SubscribeEmail function which in turn uses the http client to invoke the notifications ktor microservice
     * route to subscribe.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param email String
     * @param stageName String
     * @param statusType String
     */
    @GraphQLDescription("Subscribe Email Notifications")
    @Suppress("unused")
    fun subscribeEmail(
        dataStreamId: String,
        dataStreamRoute: String,
        email: String,
        stageName: String,
        statusType: String
    ): SubscriptionResult {
        val url = workflowServiceConnection.getUrl("/subscribe/email")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(EmailSubscription(dataStreamId, dataStreamRoute, email, stageName, statusType))
                }
                return@runBlocking ProcessResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    ProcessErrorCodes(url, e, null)
                }
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }

    /**
     * UnSubscribeEmail function which in turn uses the http client to invoke the notifications ktor microservice
     * route to unsubscribe email notifications using the subscriberId.
     *
     * @param subscriptionId String
     */
    @GraphQLDescription("Unsubscribe Email Notifications")
    @Suppress("unused")
    fun unsubscribeEmail(subscriptionId: String): SubscriptionResult {
        val url = workflowServiceConnection.getUrl("/unsubscribe/email")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(UnSubscription(subscriptionId))
                }
                return@runBlocking ProcessResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    ProcessErrorCodes(url, e, subscriptionId)
                }
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }

    /**
     * SubscribeWebhook function which in turn uses the http client to invoke the notifications ktor microservice
     * route to subscribe webhook notifications.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param email String
     * @param stageName String
     * @param statusType String
     */
    @GraphQLDescription("Subscribe Webhook Notifications")
    @Suppress("unused")
    fun subscribeWebhook(
        dataStreamId: String,
        dataStreamRoute: String,
        email: String,
        stageName: String,
        statusType: String
    ): SubscriptionResult {
        val url = workflowServiceConnection.getUrl("/subscribe/webhook")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(EmailSubscription(dataStreamId, dataStreamRoute, email, stageName, statusType))
                }
                return@runBlocking ProcessResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    ProcessErrorCodes(url, e, null)
                }
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }

    /**
     * UnSubscribeWebhook function which in turn uses the http client to invoke the notifications ktor microservice
     * route to unsubscribe webhook notifications.
     *
     * @param subscriptionId String
     */
    @GraphQLDescription("Unsubscribe Webhook Notifications")
    @Suppress("unused")
    fun unsubscribeWebhook(subscriptionId: String): SubscriptionResult {
        val url = workflowServiceConnection.getUrl("/unsubscribe/webhook")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(UnSubscription(subscriptionId))
                }
                return@runBlocking ProcessResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    ProcessErrorCodes(url, e, subscriptionId)
                }
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }

    companion object {

        /**
         * Function to process the http response coming from notifications service
         * @param response HttpResponse
         */
        private suspend fun ProcessResponse(response: HttpResponse): SubscriptionResult {
            if (response.status == HttpStatusCode.OK) {
                return response.body()
            } else {
                throw Exception("Notification service is unavailable. Status:${response.status}")
            }
        }
    }

}

/**
 * Function to process the http response codes and throw exception accordingly.
 *
 * @param url String
 * @param e Exception
 * @param subscriptionId String?
 */
@Throws(Exception::class)
internal fun ProcessErrorCodes(url: String, e: Exception, subscriptionId: String?) {
    val error = e.message!!.substringAfter("Status:").substringBefore(" ")
    when (error) {
        "500" -> throw Exception("Subscription with subscriptionId = $subscriptionId does not exist in the cache")
        "400" -> throw Exception("Bad Request: Please check the request and retry")
        "401" -> throw Exception("Unauthorized access to notifications service")
        "403" -> throw Exception("Access to notifications service is forbidden")
        "404" -> throw Exception("$url not found")
        else -> throw Exception(e.message)
    }
}