package gov.cdc.ocio.processingstatusapi.mutations.response

import gov.cdc.ocio.processingstatusapi.mutations.models.NotificationSubscriptionResult
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

object SubscriptionResponse{

    /**
     * Function to process the http response coming from notifications service
     * @param response HttpResponse
     */
    @JvmStatic
    suspend fun ProcessNotificationResponse(response: HttpResponse): NotificationSubscriptionResult {
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            throw Exception("Notification service is unavailable. Status:${response.status}")
        }
    }

    @JvmStatic
    @Throws(Exception::class)
            /**
             * Function to process the http response codes and throw exception accordingly
             * @param url String
             * @param e Exception
             * @param subscriptionId String?
             */
    fun ProcessErrorCodes(url: String, e: Exception, subscriptionId: String?) {
        val error = e.message!!.substringAfter("Status:").substringBefore(" ")
        when (error) {
            "500" -> throw Exception("Subscription with subscriptionId = ${subscriptionId} does not exist in the cache")
            "400" -> throw Exception("Bad Request: Please check the request and retry")
            "401" -> throw Exception("Unauthorized access to notifications service")
            "403" -> throw Exception("Access to notifications service is forbidden")
            "404" -> throw Exception("${url} not found")
            else -> throw Exception(e.message)
        }
    }
}
