package cache

import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType

class MockHealthCheckSystem(
    system: String,
    service: String?
) : HealthCheckSystem(system, service) {

    override fun doHealthCheck(): HealthCheckResult {
        return HealthCheckResult(
            system = system,
            service = service,
            status = HealthStatusType.STATUS_UP,
            healthIssues = null
        )
    }
}