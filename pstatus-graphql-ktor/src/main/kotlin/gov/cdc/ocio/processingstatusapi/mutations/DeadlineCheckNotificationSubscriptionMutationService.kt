package gov.cdc.ocio.processingstatusapi.mutations

import gov.cdc.ocio.processingstatusapi.mutations.models.NotificationSubscriptionResult
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.exceptions.ResponseException
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


/**
 * Deadline Check Subscription data class.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param for digest counts and the frequency with which each of the top 5 errors occur String
 * @param deliveryReference String
 */
@Serializable
data class DeadlineCheckSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String,
    val cronSchedule: String,
    val deliveryReference: String
)

/**
 * Deadline check unSubscription data class which is serialized back and forth which is in turn used for unsubscribing
 * from the cache for emails and webhooks using the given subscriberId.
 *
 * @param subscriptionId
 */
@Serializable
data class DeadlineCheckUnSubscription(val subscriptionId:String)

/**
 * The graphQL mutation service class for subscribing to and unsubscribing from the upload deadline check.
 */
class DeadlineCheckSubscriptionMutationService(
    workflowServiceUrl: String?
) : Mutation {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * The mutation function which invokes the upload deadline check microservice route to subscribe.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param cronSchedule String
     * @param deliveryReference String
     */
    @GraphQLDescription("Subscribe Deadline Check lets you get notifications when an upload from jurisdictions has not happened by 12pm")
    @Suppress("unused")
    fun subscribeDeadlineCheck(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        cronSchedule: String,
        deliveryReference: String
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/subscribe/deadlineCheck")

        return runBlocking {
            val result = runCatching {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DeadlineCheckSubscription(
                            dataStreamId,
                            dataStreamRoute,
                            jurisdiction,
                            cronSchedule,
                            deliveryReference
                        )
                    )
                }
                return@runCatching SubscriptionResponse.ProcessNotificationResponse(response)
            }
            result.onFailure {
                when (it) {
                    is ResponseException -> throw it
                    else -> throw Exception(workflowServiceConnection.serviceUnavailable)
                }
            }
            return@runBlocking result.getOrThrow()
        }
    }

    /**
     * The mutation function which invokes the upload deadline check microservice route to unsubscribe.
     *
     * @param subscriptionId String
     */
    @GraphQLDescription("UnSubscribe Deadline Check lets you unsubscribe from getting notifications when an upload from jurisdictions has not happened by 12pm")
    @Suppress("unused")
    fun unsubscribeDeadlineCheck(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/unsubscribe/deadlineCheck")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DeadlineCheckUnSubscription(subscriptionId)
                    )
                }
                return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    SubscriptionResponse.ProcessErrorCodes(url, e, null)
                }
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }

}