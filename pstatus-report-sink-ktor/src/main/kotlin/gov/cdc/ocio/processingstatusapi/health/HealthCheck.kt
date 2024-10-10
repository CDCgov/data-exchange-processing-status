package gov.cdc.ocio.processingstatusapi.health


/**
 * Run health checks for the service.
 *
 * @property status String?
 * @property totalChecksDuration String?
 * @property dependencyHealthChecks MutableList<HealthCheckSystem>
 */
class HealthCheck {

    companion object{
        const val STATUS_UP = "UP"
        const val STATUS_DOWN = "DOWN"
        const val STATUS_UNSUPPORTED = "UNSUPPORTED"
    }

    var status: String = STATUS_DOWN

    var totalChecksDuration: String? = null

    var dependencyHealthChecks = mutableListOf<HealthCheckSystem>()
}

