package gov.cdc.ocio.processingstatusapi.mutations.response

import gov.cdc.ocio.processingstatusapi.exceptions.ResponseException
import gov.cdc.ocio.processingstatusapi.mutations.models.NotificationSubscriptionResult
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable


@Serializable
data class ResponseError(
    var error: String? = null
)

object SubscriptionResponse{


    /**
     * Function to process the http response coming from notifications service
     * @param response HttpResponse
     */
    @JvmStatic
    suspend fun ProcessNotificationResponse(response: HttpResponse): WorkflowSubscriptionResult {
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            // Attempt to get the cause from the response
            val responseError = response.body<ResponseError>()
            throw ResponseException("Error: ${responseError.error}")
        }
    }

    /**
     * Function to process the http response codes and throw exception accordingly
     * @param url String
     * @param e Exception
     * @param subscriptionId String?
     */
    @Throws(Exception::class)
    fun ProcessErrorCodes(url: String, e: Exception, subscriptionId: String?) {
        val error = e.message!!.substringAfter("Status:").substringBefore(" ").trim()
        when (error) {
            "500" -> throw Exception("Subscription with subscriptionId = $subscriptionId does not exist in the cache")
            "400" -> throw Exception("Bad Request: Please check the request and retry")
            "401" -> throw Exception("Unauthorized access to notifications service")
            "403" -> throw Exception("Access to notifications service is forbidden")
            "404" -> throw Exception("$url not found")
            else -> throw Exception(e.message)
        }
    }
}
