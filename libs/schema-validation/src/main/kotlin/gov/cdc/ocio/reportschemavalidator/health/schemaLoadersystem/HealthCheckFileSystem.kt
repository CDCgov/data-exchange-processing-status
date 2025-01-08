
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.reportschemavalidator.utils.FileSystemConfiguration
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
     */
    override fun doHealthCheck() {
        try {
            if (isFileSystemHealthy(fileSystemConfiguration)) {
                status = HealthStatusType.STATUS_UP
            }

        } catch (ex: Exception) {
            logger.error("File system is not accessible and hence not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether the file system folder exists.
     *
     * @param config FileSystemConfiguration
     * @return Boolean
     * @throws FileNotFoundException
     */
    @Throws(FileNotFoundException::class)
    fun isFileSystemHealthy(config: FileSystemConfiguration): Boolean {
        if (File(config.localFileSystemPath).exists())
            return true
        else
            throw FileNotFoundException("Report schema folder '${config.localFileSystemPath}' does not exist")
    }

}
