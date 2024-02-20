package gov.cdc.ocio;

import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import gov.cdc.ocio.functions.http.SubscribeEmailNotifications;
import gov.cdc.ocio.functions.http.SubscribeWebsocketNotifications;
import gov.cdc.ocio.functions.http.UnsubscribeNotifications;

import java.util.Optional;

public class FunctionJavaWrappers {
    /**
     * Subscribe for email notifications using Rest endpoint
     * @param request HttpRequest
     * @param destinationId destinationId of the report
     * @param eventType eventType of the report
     * @return HttpResponse
     */
    @FunctionName("SubscribeEmail")
    public HttpResponseMessage subscribeEmail(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "subscribe/email/{destinationId}/{eventType}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("destinationId") String destinationId,
            @BindingName("eventType")String eventType
    ) {
        return new SubscribeEmailNotifications(request).run(destinationId, eventType);
    }

    /**
     * Subscribes for websocket notifications using Rest endpoint
     * @param request HttpRequest
     * @param destinationId destinationId of the report
     * @param eventType eventType of the report
     * @return HttpResponse
     */
    @FunctionName("SubscribeWebsocket")
    public HttpResponseMessage subscribeWebsocket(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "subscribe/websocket/{destinationId}/{eventType}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("destinationId") String destinationId,
            @BindingName("eventType") String eventType
    ) {
        return new SubscribeWebsocketNotifications(request).run(destinationId, eventType);
    }

    /**
     * Unsubscribes for given subscriptionId using Rest endpoint
     * @param request HttpRequest
     * @param subscriptionId unique identifier for subscription
     * @return HttpResponse
     */
    @FunctionName("Unsubscribe")
    public HttpResponseMessage unsubscribe(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "unsubscribe/{subscriptionId}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("subscriptionId") String subscriptionId
    ) {
        return new UnsubscribeNotifications(request).run(subscriptionId);
    }
}
