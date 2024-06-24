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

@Serializable
data class EmailSubscription(val dataStreamId:String,
                             val dataStreamRoute:String,
                             val email: String,
                             val stageName: String,
                             val statusType:String)

@Serializable
data class UnSubscription(val subscriptionId:String)


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