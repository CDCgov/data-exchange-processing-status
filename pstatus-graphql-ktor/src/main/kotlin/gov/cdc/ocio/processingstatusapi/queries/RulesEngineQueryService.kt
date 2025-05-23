package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.models.Subscription
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking


/**
 * Rules engine query service.
 *
 * @property rulesEngineServiceConnection ServiceConnection
 * @constructor
 */
class RulesEngineQueryService(
    rulesEngineServiceUrl: String?
) : Query {

    private val rulesEngineServiceConnection =
        ServiceConnection("notifications rules engine", rulesEngineServiceUrl)

    @GraphQLDescription("Retrieves all of the notification rules-engine based subscriptions. A subscription describes the conditions under which a report is scrutinized and a notification emitted if the conditions are met.")
    @Suppress("unused")
    fun getAllSubscriptions(): List<Subscription> {
        val url = rulesEngineServiceConnection.buildUrl("/subscriptions")

        return runBlocking {
            try {
                val response = rulesEngineServiceConnection.client.get(url) {
                    contentType(ContentType.Application.Json)
                }
                if (response.status == HttpStatusCode.OK) {
                    return@runBlocking Subscription.fromJsonArray(response.body())
                } else {
                    throw Exception("Service unavailable. Status: ${response.status}")
                }
            } catch (e: Exception) {
                throw Exception(rulesEngineServiceConnection.serviceUnavailable)
            }
        }
    }

    @GraphQLDescription("Retrieves the notification rules-engine based subscription from the subscription identifier provided.  A subscription describes the conditions under which a report is scrutinized and a notification emitted if the conditions are met.")
    @Suppress("unused")
    fun getSubscription(subscriptionId: String): Subscription? {
        val url = rulesEngineServiceConnection.buildUrl("/subscription/$subscriptionId")

        return runBlocking {
            try {
                val response = rulesEngineServiceConnection.client.get(url) {
                    contentType(ContentType.Application.Json)
                }
                if (response.status == HttpStatusCode.OK) {
                    return@runBlocking Subscription.fromJson(response.body())
                } else {
                    throw Exception("Service unavailable. Status: ${response.status}")
                }
            } catch (e: Exception) {
                throw Exception(rulesEngineServiceConnection.serviceUnavailable)
            }
        }
    }
}
