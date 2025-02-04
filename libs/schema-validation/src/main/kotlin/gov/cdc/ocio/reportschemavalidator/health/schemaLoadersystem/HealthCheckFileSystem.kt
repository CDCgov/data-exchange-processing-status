
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import java.io.File
import java.io.FileNotFoundException


/**
 * Concrete implementation of the file system health checks.
 */

@JsonIgnoreProperties("koin")
class HealthCheckFileSystem(
    system: String,
    private val localFileSystemPath: String
) : HealthCheckSystem(system, "file_system"), KoinComponent {

    /**
     * Checks tha the folder for the file system exists.
     *
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isFileSystemHealthy()
        result.onFailure { error ->
            val reason = "File system is not accessible and hence not healthy ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }

        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether the file system folder exists.
     *
     * @return Result<Boolean>
     */
    private fun isFileSystemHealthy(): Result<Boolean> {
        return if (File(localFileSystemPath).exists())
            Result.success(true)
        else
            Result.failure(FileNotFoundException("Report schema folder '${localFileSystemPath}' does not exist"))
    }

}
