package gov.cdc.ocio.types.health

import mu.KotlinLogging

/**
 * Abstract class used for modeling the health issues of an individual service.
 *
 * @property system String
 * @property service String
 * @property logger KLogger
 * @constructor
 */
abstract class HealthCheckSystem(
    val system: String,
    val service: String?
) {

    protected val logger = KotlinLogging.logger {}

    abstract fun doHealthCheck(): HealthCheckResult
}

