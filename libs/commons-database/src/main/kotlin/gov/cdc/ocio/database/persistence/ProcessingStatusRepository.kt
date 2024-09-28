package gov.cdc.ocio.database.persistence


/**
 * Base class for all processing status repositories
 *
 * @property reportsCollection Collection
 * @property reportsDeadLetterCollection Collection
 */
open class ProcessingStatusRepository {

    // Common interface for the reports collection
    open lateinit var reportsCollection: Collection

    // Common interface for the reports deadletter collection
    open lateinit var reportsDeadLetterCollection: Collection
}