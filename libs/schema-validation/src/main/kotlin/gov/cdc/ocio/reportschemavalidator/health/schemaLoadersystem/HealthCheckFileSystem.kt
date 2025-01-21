
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.reportschemavalidator.utils.FileSystemConfiguration
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException


/**
 * Concrete implementation of the file system health checks.
 */

@JsonIgnoreProperties("koin")
class HealthCheckFileSystem : HealthCheckSystem("file_system"), KoinComponent {

    private val fileSystemConfiguration by inject<FileSystemConfiguration>()

    /**
     * Checks tha the folder for the file system exists.
     *
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isFileSystemHealthy(fileSystemConfiguration)
        result.onFailure { error ->
            val reason = "File system is not accessible and hence not healthy ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(service, HealthStatusType.STATUS_DOWN, reason)
        }

        return HealthCheckResult(service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether the file system folder exists.
     *
     * @param config FileSystemConfiguration
     * @return Result<Boolean>
     */
    private fun isFileSystemHealthy(config: FileSystemConfiguration): Result<Boolean> {
        return if (File(config.localFileSystemPath).exists())
            Result.success(true)
        else
            Result.failure(FileNotFoundException("Report schema folder '${config.localFileSystemPath}' does not exist"))
    }

}
