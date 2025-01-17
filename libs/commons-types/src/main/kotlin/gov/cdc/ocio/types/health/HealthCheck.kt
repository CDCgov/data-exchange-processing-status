package gov.cdc.ocio.types.health


/**
 * Run health checks for the service.
 *
 * @property status HealthStatusType
 * @property totalChecksDuration String?
 * @property dependencyHealthChecks MutableList<HealthCheckSystem>
 */
class HealthCheck {

    var status = HealthStatusType.STATUS_DOWN

    var totalChecksDuration: String? = null

    var dependencyHealthChecks = mutableListOf<HealthCheckResult>()
}

