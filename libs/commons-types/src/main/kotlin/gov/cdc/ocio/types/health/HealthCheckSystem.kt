package gov.cdc.ocio.types.health

import mu.KotlinLogging


/**
 * Abstract class used for modeling the health issues of an individual service.
 *
 * @property status String
 * @property healthIssues String?
 * @property service String
 */
abstract class HealthCheckSystem(val service: String) {

    protected val logger = KotlinLogging.logger {}

    var status = HealthStatusType.STATUS_DOWN
        protected set

    var healthIssues: String? = null
        protected set

    abstract fun doHealthCheck()
}