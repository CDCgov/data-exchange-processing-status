/*



import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.micronaut.graphql.annotation.GraphQLMutation
import jakarta.inject.Singleton


// Define the input and output types
data class SubscribeDeadlineCheckInput(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String,
    val daysToRun: List<String>,
    val timeToRun: String,
    val deliveryReference: String
)

data class SubscriptionResponse(
    val subscriptionId: String,
    val success: Boolean,
    val message: String?
)

data class UnsubscriptionResponse(
    val success: Boolean,
    val message: String?
)

// Implement the resolver
@Singleton
class SubscriptionResolver(private val workflowClient: WorkflowClient) {

    @GraphQLMutation
    fun subscribeDeadlineCheck(input: SubscribeDeadlineCheckInput): SubscriptionResponse {
        val options = WorkflowOptions.newBuilder()
            .setWorkflowId("UploadCheckWorkflow-${input.dataStreamId}")
            .build()

        val workflow = workflowClient.newWorkflowStub(NotificationWorkflow::class.java, options)

        WorkflowClient.start(
            workflow::checkUploadAndNotify,
            input.dataStreamId,
            input.dataStreamRoute,
            input.jurisdiction,
            input.daysToRun,
            input.timeToRun,
            input.deliveryReference
        )

        return SubscriptionResponse(
            subscriptionId = "UploadCheckWorkflow-${input.dataStreamId}",
            success = true,
            message = "Subscription successful"
        )
    }

    @GraphQLMutation
    fun unsubscribeDeadlineCheck(subscriptionId: String): UnsubscriptionResponse {
        // Implement the unsubscription logic using WorkflowClient
        return UnsubscriptionResponse(
            success = true,
            message = "Unsubscription successful"
        )
    }
}
*/
