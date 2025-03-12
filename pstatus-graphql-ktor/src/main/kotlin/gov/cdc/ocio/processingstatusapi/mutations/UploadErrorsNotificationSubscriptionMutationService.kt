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
 * Upload errors subscription data class.
 *
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param cronSchedule List<String>
 * @param emailAddresses List<String>
 */
@Serializable
data class UploadErrorsNotificationSubscription(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String,
    val cronSchedule: String,
    val emailAddresses: List<String>
)

/**
 * Upload errors unSubscription data class which is serialized back and forth which is in turn used for unsubscribing
 * from the cache for emails and webhooks using the given subscriberId.
 *
 * @param subscriptionId
 */
@Serializable
data class UploadErrorsNotificationUnSubscription(val subscriptionId:String)

/**
 * The graphQL mutation service class for upload errors notification subscription/unSubscription
 */
class UploadErrorsNotificationSubscriptionMutationService(
    workflowServiceUrl: String?
) : Mutation {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * The mutation function which invokes the upload errors notification microservice route to subscribe to it.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param cronSchedule String
     * @param emailAddresses List<String>
     */
    @GraphQLDescription("Subscribe upload errors lets you get notifications when there are errors in an upload")
    @Suppress("unused")
    fun subscribeUploadErrorsNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        cronSchedule: String,
        emailAddresses: List<String>
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/subscribe/uploadErrorsNotification")

        return runBlocking {
            val result = runCatching {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UploadErrorsNotificationSubscription(
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
     * The mutation function which invokes the upload errors in the upload microservice route to unsubscribe.
     *
     * @param subscriptionId String
     */
    @GraphQLDescription("UnSubscribe upload errors lets you unsubscribe from getting notifications when there are errors during an upload")
    @Suppress("unused")
    fun unsubscribeUploadErrorsNotification(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/unsubscribe/uploadErrorsNotification")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UploadErrorsNotificationUnSubscription(subscriptionId)
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