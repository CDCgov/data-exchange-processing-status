package gov.cdc.ocio;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.*;
import gov.cdc.ocio.function.http.SubscribeEmailNotifications;
import gov.cdc.ocio.function.http.SubscribeWebsocketNotifications;
import gov.cdc.ocio.function.http.UnsubscribeNotifications;
import gov.cdc.ocio.function.servicebus.ReportsNotificationsSBQueueProcessor;

import java.util.Optional;

public class FunctionJavaWrappers {
    /**
     * Subscribe for email notifications using Rest endpoint
     * @param request HttpRequest
     * @param dataStreamId dataStreamId of the report
     * @param dataStreamRoute dataStreamRoute of the report
     * @return HttpResponse
     */
    @FunctionName("SubscribeEmail")
    public HttpResponseMessage subscribeEmail(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "subscribe/email/{dataStreamId}/{dataStreamRoute}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("dataStreamId") String dataStreamId,
            @BindingName("dataStreamRoute")String dataStreamRoute
    ) {
        return new SubscribeEmailNotifications(request).run(dataStreamId, dataStreamRoute);
    }

    /**
     * Subscribes for websocket notifications using Rest endpoint
     * @param request HttpRequest
     * @param dataStreamId dataStreamId of the report
     * @param dataStreamRoute dataStreamRoute of the report
     * @return HttpResponse
     */

    @FunctionName("SubscribeWebsocket")
    public HttpResponseMessage subscribeWebsocket(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "subscribe/websocket/{dataStreamId}/{dataStreamRoute}",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("dataStreamId") String dataStreamId,
            @BindingName("dataStreamRoute") String dataStreamRoute
    ) {
        return new SubscribeWebsocketNotifications(request).run(dataStreamId, dataStreamRoute);
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

    /***
     * Process a message from the service bus queue.
     *
     * @param message JSON message content
     * @param context Execution context of the service bus message
     */
    @FunctionName("ServiceBusProcessor")
    public void serviceBusProcessor(
            @ServiceBusQueueTrigger(
                    name = "msg",
                    queueName = "%ReportNotificationSBQueueName%",
                    connection = "ServiceBusConnectionString"
            ) String message,
            final ExecutionContext context
    ) {
        try {
            context.getLogger().info("Received message: " + message);
            new ReportsNotificationsSBQueueProcessor(context).withMessage(message);
        } catch (Exception e) {
            context.getLogger().warning("Failed to process service bus message: " + e.getLocalizedMessage());
        }
    }
}
