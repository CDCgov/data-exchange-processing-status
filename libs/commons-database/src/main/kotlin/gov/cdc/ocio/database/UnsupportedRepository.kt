package gov.cdc.ocio.database

import gov.cdc.ocio.database.health.HealthCheckUnsupportedDb
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * Unsupported repository implementation.
 *
 * @property healthCheckSystem HealthCheckSystem
 * @constructor
 */
class UnsupportedRepository(
    databaseName: String
) : ProcessingStatusRepository() {

    override var healthCheckSystem = HealthCheckUnsupportedDb(databaseName) as HealthCheckSystem
}
