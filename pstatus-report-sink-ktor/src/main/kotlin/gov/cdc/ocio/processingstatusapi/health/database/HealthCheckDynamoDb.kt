package gov.cdc.ocio.processingstatusapi.health.database

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.processingstatusapi.health.HealthCheckSystem
import org.koin.core.component.KoinComponent


/**
 * Concrete implementation of the dynamodb health check.
 */
@JsonIgnoreProperties("koin")
class HealthCheckDynamoDb: HealthCheckSystem("Dynamo DB"), KoinComponent {
    override fun doHealthCheck() {
        TODO("Not yet implemented")
    }
}