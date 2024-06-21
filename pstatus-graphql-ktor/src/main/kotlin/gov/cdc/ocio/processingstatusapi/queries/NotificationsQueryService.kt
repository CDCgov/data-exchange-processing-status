package gov.cdc.ocio.processingstatusapi.queries



import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.dataloaders.ReportDeadLetterDataLoader
import gov.cdc.ocio.processingstatusapi.loaders.ReportDeadLetterLoader
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture

class NotificationsQueryService : Query {

    @GraphQLDescription("Return all the subscriptions")
    @Suppress("unused")
    fun getSubscriptionsByRuleId(ruleId: String) = ReportDeadLetterLoader().getByUploadId(ruleId)

    @GraphQLDescription("Return all the subscriptions")
    @Suppress("unused")
    fun getSubscriptionById(subscriptionId: String) = ReportDeadLetterLoader().getByUploadId(subscriptionId)
}

