package gov.cdc.ocio.functions
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import java.util.Optional

class FunctionKotlinWrappers {
    @FunctionName("SubscribeEmail")
    fun subscribeEmail(
        @HttpTrigger(
                    name = "req",
                    methods = [HttpMethod.POST],
                    route = "subscribe/email/{destinationId}/{eventType}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("destinationId") destinationId: String,
        @BindingName("eventType") eventType: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        context.logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        return SubscribeEmailNotifications().run(request, destinationId, eventType, context);
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
        @BindingName("eventType") eventType: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        context.logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        return SubscribeWebsocketNotifications().run(request, destinationId, eventType, context);
    }

    @FunctionName("Unsubscribe")
    fun unsubscribe(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.PUT],
            route = "unsubscribe/{notificationType}/{subscriptionId}",
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("notificationType") notificationType: String,
        @BindingName("subscriptionId") subscriptionId: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        context.logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        return UnsubscribeNotifications().run(request, notificationType, subscriptionId, context);
    }

}
