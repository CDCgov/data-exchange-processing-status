@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

/**
 * Email Subscription data class which is serialized back and forth which is in turn subscribed in to the MemCache
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param email String
 * @param stageName String
 * @param statusType String
 */
@Serializable
data class EmailSubscription(val dataStreamId:String,
                             val dataStreamRoute:String,
                             val email: String,
                             val stageName: String,
                             val statusType:String)

/**
 * UnSubscription data class which is serialized back and forth which is in turn used for unsubscribing  from the cache for emails and webhooks using the given subscriberId
 * @param subscriptionId
 */
@Serializable
data class UnSubscription(val subscriptionId:String)

/**
 * SubscriptionResult is the response class which is serialized back and forth which is in turn used for getting the response which contains the subscriberId , message and the status of subscribe/unsubscribe operations
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

class NotificationsMutationService : Mutation {
    val notificationsRouteBaseUrl =System.getenv("PSTATUS_NOTIFICATIONS_BASE_URL")
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }

    }

    /**
     *  SubscribeEmail function which inturn uses the http client to invoke the notifications ktor microservice route to subscribe
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param email String
     * @param stageName String
     * @param statusType String
     */

    @GraphQLDescription("Subscribe Email Notifications")
    @Suppress("unused")
    fun subscribeEmail(dataStreamId:String, dataStreamRoute:String,email: String, stageName: String, statusType:String):SubscriptionResult {
        val url = "$notificationsRouteBaseUrl/subscribe/email"
        return runBlocking {
             client.post(url) {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(EmailSubscription(dataStreamId, dataStreamRoute,email,stageName,statusType))
            }.body()

        }

    }

    /**
     *  UnSubscribeEmail function which inturn uses the http client to invoke the notifications ktor microservice route to unsubscribe email notifications using the subscriberId
     * @param subscriptionId String
     */

    @GraphQLDescription("Unsubscribe Email Notifications")
    @Suppress("unused")
    fun unsubscribeEmail(subscriptionId:String):SubscriptionResult {
        val url = "$notificationsRouteBaseUrl/unsubscribe/email"
        return runBlocking {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(UnSubscription(subscriptionId))
            }.body()
        }

    }

    /**
     *  SubscribeWebhook function which inturn uses the http client to invoke the notifications ktor microservice route to subscribe webhook notifications
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param email String
     * @param stageName String
     * @param statusType String
     */

    @GraphQLDescription("Subscribe Webhook Notifications")
    @Suppress("unused")
    fun subscribeWebhook(dataStreamId:String, dataStreamRoute:String,email: String, stageName: String, statusType:String):SubscriptionResult {
        val url = "$notificationsRouteBaseUrl/subscribe/webhook"
        return runBlocking {
            client.post(url) {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(EmailSubscription(dataStreamId, dataStreamRoute,email,stageName,statusType))
            }.body()

        }

    }

    /**
     *  UnSubscribeWebhook function which inturn uses the http client to invoke the notifications ktor microservice route to unsubscribe webhook notifications
     * @param subscriptionId String
     */

    @GraphQLDescription("Unsubscribe Webhook Notifications")
    @Suppress("unused")
    fun unsubscribeWebhook(subscriptionId:String):SubscriptionResult {
        val url = "$notificationsRouteBaseUrl/unsubscribe/webhook"
        return runBlocking {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(UnSubscription(subscriptionId))
            }.body()
        }

    }
}