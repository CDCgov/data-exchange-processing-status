package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.exceptions.ResponseException
import gov.cdc.ocio.processingstatusapi.mutations.models.NotificationSubscriptionResult
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


/**
 * DataStream Subscription for digest counts and top errors.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param cronSchedule String
 * @param emailAddresses List<String>
 */
@Serializable
data class DataStreamTopErrorsNotificationSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String,
    val cronSchedule: String,
    val emailAddresses: List<String>
)

/**
 * DataStream UnSubscription data class used for unsubscribing from the db for digest counts and the top errors and
 * their frequency within an upload.
 *
 * @param subscriptionId
 */
@Serializable
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId:String)

/**
 * The graphQL mutation class for dataStream Subscription for digest counts and top5 errors and their frequencies.
 */
class DataStreamTopErrorsNotificationSubscriptionMutationService(
    workflowServiceUrl: String?
) : Mutation {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * The mutation function which invokes the data stream top errors and digest counts microservice route to subscribe.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param cronSchedule String
     * @param emailAddresses List<String>
     */
    @GraphQLDescription("Subscribe data stream top errors lets you subscribe to get notifications for top data stream errors and its frequency during an upload")
    @Suppress("unused")
    fun subscribeDataStreamTopErrorsNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        cronSchedule: String,
        emailAddresses: List<String>
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/subscribe/dataStreamTopErrorsNotification")

        return runBlocking {
            val result = runCatching {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DataStreamTopErrorsNotificationSubscription(
                            dataStreamId,
                            dataStreamRoute,
                            jurisdiction,
                            cronSchedule,
                            emailAddresses
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
     * The mutation function which invokes the data stream top errors and digest counts microservice route to
     * unsubscribe.
     *
     * @param subscriptionId String
    */
    @GraphQLDescription("UnSubscribe data stream top errors lets you unsubscribe from getting notifications for top data stream errors and its frequency during an upload")
    @Suppress("unused")
    fun unsubscribesDataStreamTopErrorsNotification(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/unsubscribe/dataStreamTopErrorsNotification")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DataStreamTopErrorsNotificationUnSubscription(subscriptionId)
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