package gov.cdc.ocio.processingstatusapi.mutations.models

import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult

/**
 * Structure a successfully validated report.
 *
 * @property validationSchemaResult ValidationSchemaResult?
 * @property report Report?
 * @constructor
 */
data class ValidatedReportResult(
    var validationSchemaResult: ValidationSchemaResult? = null,
    var report: Report? = null
)