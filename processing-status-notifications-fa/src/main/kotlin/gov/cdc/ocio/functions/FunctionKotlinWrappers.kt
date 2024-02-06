package gov.cdc.ocio.functions
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.ocio.functions.http.SubscribeEmailNotifications
import gov.cdc.ocio.functions.http.SubscribeWebsocketNotifications
import gov.cdc.ocio.functions.http.UnsubscribeNotifications
import java.util.*

class FunctionKotlinWrappers {
    // Http Triggers for REST Calls
    @FunctionName("SubscribeEmail")
    fun subscribeEmail(
        @HttpTrigger(
                    name = "req",
                    methods = [HttpMethod.POST],
                    route = "subscribe/email/{destinationId}/{eventType}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("destinationId") destinationId: String,
        @BindingName("eventType") eventType: String
    ): HttpResponseMessage {
        return SubscribeEmailNotifications(request).run(destinationId, eventType)
    }

    @FunctionName("SubscribeWebsocket")
    fun subscribeWebsocket(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            route = "subscribe/websocket/{destinationId}/{eventType}",
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("destinationId") destinationId: String,
        @BindingName("eventType") eventType: String
    ): HttpResponseMessage {
        return SubscribeWebsocketNotifications(request).run(destinationId, eventType)
    }

    @FunctionName("Unsubscribe")
    fun unsubscribe(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.PUT],
            route = "unsubscribe/{notificationType}/{subscriptionId}",
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("subscriptionId") subscriptionId: String
    ): HttpResponseMessage {
        return UnsubscribeNotifications(request).run(subscriptionId)
    }
}
