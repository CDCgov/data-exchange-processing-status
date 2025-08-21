package gov.cdc.ocio.processingstatusapi.mutations.response

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ForbiddenException
import gov.cdc.ocio.processingstatusapi.exceptions.ResponseException
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable


@Serializable
data class ResponseError(
    var error: String? = null
)

object SubscriptionResponse {

    /**
     * Function to process the http response coming from either of the notifications services.
     *
     * @param response HttpResponse
     */
    @JvmStatic
    suspend inline fun<reified T> ProcessNotificationResponse(response: HttpResponse): T {
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            // Attempt to get the cause from the response
            val responseError = response.body<ResponseError>()
            val error = (responseError.error ?: "Unknown")
            throw when(response.status) {
                HttpStatusCode.BadRequest -> BadRequestException(error)
                HttpStatusCode.Forbidden -> ForbiddenException(error)
                HttpStatusCode.Unauthorized -> Exception("Unauthorized: $error")
                HttpStatusCode.NotFound -> NotFoundException(error)
                HttpStatusCode.InternalServerError -> Exception("Internal server error: $error")
                else -> ResponseException(error)
            }
        }
    }
}
