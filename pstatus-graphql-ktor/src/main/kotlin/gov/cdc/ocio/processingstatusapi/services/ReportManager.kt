package gov.cdc.ocio.processingstatusapi.services

import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Manages incoming reports for creation or replacement.
 *
 * @property logger KLogger
 * @property repository ProcessingStatusRepository
 * @property reportsCollection Collection
 */
class ReportManager: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val reportsCollection = repository.reportsCollection

    /**
     * Creates a new report in the database.
     *
     * This function generates a new ID for the report, validates the provided upload ID,
     * and attempts to create the report in the Cosmos DB container. If the report ID
     * is provided or if the upload ID is missing, a BadRequestException is thrown.
     * If there is an error during the creation process, a ContentException is thrown.
     *
     * @param report The report object to be created. Must not have an existing ID and must include a valid upload ID.
     * @return The created Report object, or null if the creation fails.
     * @throws BadRequestException If the report ID is provided or if the upload ID is missing.
     * @throws ContentException If there is an error during the report creation process.
     */
    @Throws(BadRequestException::class, ContentException::class)
    fun createReport(report: Report) { // TODO - move to ReportManager.kt
        logger.info("Creating report for uploadId = ${report.uploadId} with stageName = ${report.stageInfo?.action}")
        val transformedContent = repository.contentTransformer(report.content as Map<*, *>)
        report.content = transformedContent

        try {
            val success =
                reportsCollection.createItem(
                    report.id!!,
                    report,
                    Report::class.java,
                    report.uploadId
                )

            // Check if successful and throw an exception if not
            if (!success)
                throw ContentException("Failed to create report")

        } catch (e: Exception) {
            logger.error(e.localizedMessage)
            throw ContentException("Failed to create report: ${e.message}")
        }
    }

    /**
     * Replaces an existing report in the database.
     *
     * This function checks if the report ID is provided and validates the upload ID.
     * It attempts to read the existing report from the Cosmos DB container and,
     * if found, replaces it with the new report data. If the report ID is missing,
     * or if the upload ID is not provided, a BadRequestException is thrown. If
     * the report is not found for replacement, another BadRequestException is thrown.
     * In case of any error during the database operations, an appropriate exception
     * will be thrown.
     *
     * @param report The report object containing the new data. Must have a valid ID and upload ID.
     * @return The updated Report object, or null if the replacement fails.
     * @throws BadRequestException If the report ID is missing, the upload ID is missing, or the report is not found.
     */
    @Throws(BadRequestException::class, ContentException::class)
    fun replaceReport(report: Report) { //TODO- move to ReportManager.kt and update
        return try {
            val uploadId = report.uploadId
            val stageInfo = report.stageInfo

            // Delete all reports matching the report ID with the same service and action name
            val cName = repository.reportsCollection.collectionNameForQuery
            val cVar = repository.reportsCollection.collectionVariable
            val cPrefix = repository.reportsCollection.collectionVariablePrefix
            val cElFunc = repository.reportsCollection.collectionElementForQuery
            val sqlQuery = (
                    "select * from $cName $cVar "
                            + "where ${cPrefix}uploadId = '$uploadId' "
                            + "and ${cPrefix}stageInfo.${cElFunc("service")} = '${stageInfo?.service}' "
                            + "and ${cPrefix}stageInfo.${cElFunc("action")} = '${stageInfo?.action}'"
                    )
            val items = repository.reportsCollection.queryItems(
                sqlQuery,
                Report::class.java
            )
            if (items.isNotEmpty()) {
                try {
                    items.forEach {
                        reportsCollection.deleteItem(
                            it.id,
                            it.uploadId
                        )
                    }
                    logger.info("Removed all reports with stage name = $stageInfo?.stage")
                } catch (e: Exception) {
                    throw BadStateException("Issue deleting report: ${e.localizedMessage}")
                }
            }

            createReport(report)

        } catch (e: Exception) {
            logger.error(e.localizedMessage)
            throw ContentException("Failed to replace report: ${e.message}")
        }
    }
}