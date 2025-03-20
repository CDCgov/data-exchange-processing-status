package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.messagesystem.exceptions.BadRequestException
import gov.cdc.ocio.messagesystem.exceptions.BadStateException
import gov.cdc.ocio.messagesystem.models.DispositionType
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.models.ReportDeadLetter
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.messagesystem.models.Source
import gov.cdc.ocio.processingstatusapi.models.MessageMetadataSB
import gov.cdc.ocio.processingstatusapi.models.StageInfoSB
import mu.KotlinLogging
import org.apache.commons.lang3.SerializationUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*
import com.fasterxml.jackson.databind.JsonNode


/**
 * The report manager interacts directly with CosmosDB to persist and retrieve reports.
 *
 * @constructor
 */
class ReportManager: KoinComponent {

    private val repository by inject<ProcessingStatusRepository>()

    private val logger = KotlinLogging.logger {}


    /**
     * Create a report located with the provided upload ID.
     *
     * @param reportSchemaVersion String
     * @param reportId String
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dexIngestDateTime Instant
     * @param messageMetadata MessageMetadata?
     * @param stageInfo StageInfo?
     * @param tags Map<String, String>?
     * @param data Map<String, String>?
     * @param contentType String
     * @param content Any?
     * @param jurisdiction String?
     * @param senderId String?
     * @param dataProducerId String?
     * @param dispositionType DispositionType
     * @param source Source
     * @return String
     * @throws BadStateException
     * @throws BadRequestException
     */
    @Throws(BadStateException::class, BadRequestException::class)
    fun createReportWithUploadId(
        reportSchemaVersion:String,
        reportId:String?,
        uploadId: String,
        dataStreamId: String,
        dataStreamRoute: String,
        dexIngestDateTime: Instant,
        messageMetadata: MessageMetadataSB?,
        stageInfo: StageInfoSB?,
        tags: Map<String, String>?,
        data: Map<String, String>?,
        contentType: String,
        content: Any?,
        jurisdiction: String?,
        senderId:String?,
        dataProducerId: String?,
        dispositionType: DispositionType,
        source: Source?
    ): String {
        if (System.getProperty("isTestEnvironment") != "true") {
            return createReport(
                reportSchemaVersion,
                reportId,
                uploadId,
                dataStreamId,
                dataStreamRoute,
                dexIngestDateTime,
                messageMetadata,
                stageInfo,
                tags,
                data,
                contentType,
                content,
                jurisdiction,
                senderId,
                dataProducerId,
                dispositionType,
                source
            )
        }
        return uploadId // this is just as a fallback
    }

    /**
     * Create the provided report.  Note the dispositionType indicates whether this will add or replace existing
     * report(s) with this stageName.
     *
     * @param reportSchemaVersion: String
     * @param reportId String
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dexIngestDateTime Instant
     * @param messageMetadata MessageMetadata?
     * @param stageInfo StageInfo?
     * @param tags Map<String, String>?
     * @param data Map<String, String>?
     * @param contentType String
     * @param content Any?
     * @param jurisdiction String?
     * @param senderId String?
     * @param dataProducerId String?
     * @param dispositionType DispositionType - indicates whether to add or replace any existing reports for the given stageName.
     * @param source Source
     * @return String - report identifier
     */
    private fun createReport(
        reportSchemaVersion: String,
        reportId:String?,
        uploadId: String,
        dataStreamId: String,
        dataStreamRoute: String,
        dexIngestDateTime: Instant,
        messageMetadata: MessageMetadataSB?,
        stageInfo: StageInfoSB?,
        tags: Map<String,String>?,
        data:Map<String,String>?,
        contentType: String,
        content: Any?,
        jurisdiction: String?,
        senderId:String?,
        dataProducerId: String?,
        dispositionType: DispositionType,
        source: Source?
    ): String {

        when (dispositionType) {
            DispositionType.REPLACE -> {
                logger.info("Replacing report(s) with service = '${stageInfo?.service}', action = '${stageInfo?.action}'")
                val report = Report().apply {
                    this.id= reportId
                    this.reportSchemaVersion= reportSchemaVersion
                    this.reportId= reportId
                    this.uploadId = uploadId
                    this.dataStreamId = dataStreamId
                    this.dataStreamRoute = dataStreamRoute
                    this.dexIngestDateTime = dexIngestDateTime
                    this.jurisdiction = jurisdiction
                    this.senderId = senderId
                    this.messageMetadata = messageMetadata?.toMessageMetadata()
                    this.stageInfo = stageInfo?.toStageInfo()
                    this.tags = tags
                    this.data = data
                    this.jurisdiction = jurisdiction
                    this.senderId = senderId
                    this.dataProducerId= dataProducerId
                    this.source = source.toString()
                    this.contentType = contentType
                    this.content = getContent(contentType, content)
                }
                return replaceReport(report)

            }
            DispositionType.ADD -> {
                 val stageReportId = UUID.randomUUID().toString()
                logger.info("Creating report for stage name = $stageInfo?.stage")
                return createStageReport(
                    reportSchemaVersion,
                    stageReportId,
                    uploadId,
                    dataStreamId,
                    dataStreamRoute,
                    dexIngestDateTime,
                    messageMetadata,
                    stageInfo,
                    tags,
                    data,
                    contentType,
                    content,
                    jurisdiction,
                    senderId,
                    dataProducerId,
                    source
                )
            }
        }
    }

    /**
     * Creates a report for the given stage.
     *
     * @param reportSchemaVersion String
     * @param reportId String
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dexIngestDateTime Instant
     * @param messageMetadata MessageMetadata?
     * @param stageInfo StageInfo?
     * @param tags Map<String, String>?
     * @param data Map<String, String>?
     * @param contentType String
     * @param content Any?
     * @param jurisdiction String?
     * @param senderId String?
     * @param dataProducerId String?
     * @param source Source
     * @return String - report identifier
     * @throws BadStateException
     */
    @Throws(BadStateException::class)
    private fun createStageReport(
        reportSchemaVersion: String,
        reportId:String,
        uploadId: String,
        dataStreamId: String,
        dataStreamRoute: String,
        dexIngestDateTime: Instant,
        messageMetadata: MessageMetadataSB?,
        stageInfo: StageInfoSB?,
        tags: Map<String,String>?,
        data:Map<String,String>?,
        contentType: String,
        content: Any?,
        jurisdiction: String?,
        senderId:String?,
        dataProducerId: String?,
        source: Source?
    ): String {

        val stageReport = Report().apply {
            this.reportSchemaVersion= reportSchemaVersion
            this.id = reportId
            this.uploadId = uploadId
            this.reportId = reportId
            this.dataStreamId = dataStreamId
            this.dataStreamRoute = dataStreamRoute
            this.dexIngestDateTime = dexIngestDateTime
            this.jurisdiction = jurisdiction
            this.senderId = senderId
            this.messageMetadata = messageMetadata?.toMessageMetadata()
            this.stageInfo = stageInfo?.toStageInfo()
            this.tags = tags
            this.data = data
            this.jurisdiction = jurisdiction
            this.senderId = senderId
            this.dataProducerId= dataProducerId
            this.source = source.toString()
            this.contentType = contentType
            this.content = getContent(contentType, content)?.toString()
        }
        return createReportItem(uploadId, reportId, stageReport)
    }

    /**
     * The function which initiates the replacing of the reports based on the uploadId, stageInfo
     * service and action of the incoming report
     * @param report Report
     * @return reportId String
     */

    private fun replaceReport(report: Report):String{

        val uploadId = report.uploadId
        val stageInfo = report.stageInfo ?: throw BadRequestException("Missing stage info")
        val service = stageInfo.service ?: throw BadRequestException("Missing service name")
        val action = stageInfo.action ?: throw BadRequestException("Missing action name")

        val cName = repository.reportsCollection.collectionNameForQuery
        val cVar = repository.reportsCollection.collectionVariable
        val cPrefix = repository.reportsCollection.collectionVariablePrefix
        val cElFunc = repository.reportsCollection.collectionElementForQuery
        val sqlQuery = (
                "select * from $cName $cVar "
                        + "where ${cPrefix}uploadId = '$uploadId' "
                        + "and ${cPrefix}stageInfo.${cElFunc("service")} = '${service}' "
                        + "and ${cPrefix}stageInfo.${cElFunc("action")} = '${action}'"
                )
        val items = repository.reportsCollection.queryItems(
            sqlQuery,
            Report::class.java
        )
        if (items.isNotEmpty()) {
            try {
                logger.info("Removed all reports with stage name = $stageInfo?.stage")

             /*
                if(repository is CouchbaseRepository) {
                    (repository as CouchbaseRepository).runTransaction {ctx -> handleReportReplacement(ctx, report, items )}
                }
                else */
                    handleReportReplacementNonTransactional(report,items)

            } catch(e: Exception) {
                throw BadStateException("Issue deleting report: ${e.localizedMessage}")
            }
        }
        return report.reportId!!

    }

    /**
     * This is the function which would do the following which includes the couchbase transaction
     * Search for all report IDs that match the replacement criteria, which looks for all reports matching the provided upload id,
     * stage service name and action.
     * If we have precisely ONE report ID from step 1, we do the upsert.
     * Otherwise, all reports except for one are deleted and the remaining one is upserted.
     * @param ctx TransactionAttemptContext
     * @param newReport Report
     * @param existingReports List<Report>
     */
   /* private fun handleReportReplacement(
        ctx: TransactionAttemptContext,
        newReport:Report,
        existingReports:List<Report>) {

        val couchbaseRepository= repository as CouchbaseRepository
        val collection= couchbaseRepository.reportsCollection as CouchbaseCollection

        val timestamp = newReport.timestamp
        when (existingReports.size) {
            0 -> collection.upsertItem(ctx, newReport.reportId!!, newReport)
            1 -> {
                val existingReport = existingReports.first()
                if (existingReport.timestamp < timestamp) {
                    collection.upsertItem(ctx, newReport.reportId!!, newReport)
                } else {
                    logger.info("Skipping update: New report is older than existing report")
                }
            }

            else -> {
                val sortedReports = existingReports.sortedByDescending { it.timestamp }
                val reportToKeep = sortedReports.first()
                val reportsToDelete = sortedReports.drop(1)

                reportsToDelete.forEach { collection.deleteItem(ctx, it.reportId!!) }
                if (reportToKeep.timestamp < timestamp) {
                    collection.upsertItem(ctx, newReport.reportId!!, newReport)
                }
            }
        }
    }*/
    /**
     * This is the function which would do the following (non-transactional)
     * Search for all report IDs that match the replacement criteria, which looks for all reports matching the provided upload id,
     * stage service name and action.
     * If we have precisely ONE report ID from step 1, we do the upsert.
     * Otherwise, all reports except for one are deleted and the remaining one does upsert.
     * @param newReport Report
     * @param existingReports List<Report>
     */
        private fun handleReportReplacementNonTransactional(
            newReport: Report,
            existingReports: List<Report>
            ) {

            val timestamp = newReport.timestamp
            when (existingReports.size) {
                0 -> createReportItem(newReport.uploadId, newReport.reportId!!, newReport)
                1 -> {
                    val existingReport = existingReports.first()
                    if (existingReport.timestamp < timestamp) {
                        createReportItem(newReport.uploadId, newReport.reportId!!, newReport)
                    } else {
                        logger.info("Skipping update: New report is older than existing report")
                    }
                }
                else -> {
                    val sortedReports = existingReports.sortedByDescending { it.timestamp }
                    val reportToKeep = sortedReports.first()
                    val reportsToDelete = sortedReports.drop(1)

                    reportsToDelete.forEach { repository.reportsCollection.deleteItem(it.id,it.uploadId) }
                    if (reportToKeep.timestamp < timestamp) {
                        createReportItem(newReport.uploadId, newReport.reportId!!, newReport)
                    }
                }
            }

        }

        /**
         * Creates a dead-letter report if there is a malformed data or missing required fields
         *
         * @param uploadId String?
         * @param dataStreamId String?
         * @param dataStreamRoute String?
         * @param dexIngestDateTime Instant?
         * @param messageMetadata MessageMetadataSB?
         * @param stageInfo StageInfoSB?
         * @param tags Map<String, String>?
         * @param data Map<String, String>?
         * @param dispositionType DispositionType
         * @param contentType String?
         * @param content Any?
         * @param jurisdiction String?
         * @param senderId String?
         * @param deadLetterReasons List<String>
         * @param validationSchemaFileNames List<String>
         * @param dataProducerId String?
         * @return String
         * @throws BadStateException
         */
        @Throws(BadStateException::class)
        fun createDeadLetterReport(
            reportSchemaVersion: String?,
            uploadId: String?,
            dataStreamId: String?,
            dataStreamRoute: String?,
            dexIngestDateTime: Instant?,
            messageMetadata: MessageMetadataSB?,
            stageInfo: StageInfoSB?,
            tags: Map<String,String>?,
            data:Map<String,String>?,
            dispositionType: DispositionType,
            contentType: String?,
            content: Any?,
            jurisdiction: String?,
            senderId:String?,
            dataProducerId: String?,
            source: Source?,
            deadLetterReasons: List<String>,
            validationSchemaFileNames:List<String>
        ): String {

            val deadLetterReportId = UUID.randomUUID().toString()
            val deadLetterReport = ReportDeadLetter().apply {
                this.id = deadLetterReportId
                this.reportSchemaVersion= reportSchemaVersion
                this.uploadId = uploadId
                this.reportId = deadLetterReportId
                this.dataStreamId = dataStreamId
                this.dataStreamRoute = dataStreamRoute
                this.dexIngestDateTime = dexIngestDateTime
                this.jurisdiction = jurisdiction
                this.senderId = senderId
                this.messageMetadata = messageMetadata?.toMessageMetadata()
                this.stageInfo = stageInfo?.toStageInfo()
                this.tags = tags
                this.data = data
                this.jurisdiction = jurisdiction
                this.senderId = senderId
                this.dataProducerId = dataProducerId
                this.source = source.toString()
                this.dispositionType = dispositionType.toString()
                this.contentType = contentType
                this.deadLetterReasons= deadLetterReasons
                this.validationSchemas= validationSchemaFileNames
                this.content = (contentType?.let { getContent(contentType, content) } as JsonNode?)?.toString()
            }

            return createReportItem(uploadId, deadLetterReportId, deadLetterReport)
        }

        /**
         * Attempt to determine the content and transform it to the type needed by the database.  If the content is not
         * JSON then it will be persisted as a base64 encoded string.
         *
         * @param contentType String?
         * @param content Any?
         * @return Any?
         */
        private fun getContent(contentType: String?, content: Any?): Any? {
            if (contentType?.lowercase() == "application/json" || contentType?.lowercase() == "json") {
                try {
                    if (content is Map<*, *>)
                        return repository.contentTransformer(content)
                    else
                        throw BadStateException("Failed to interpret provided content as a JSON string")
                } catch (e: Exception) {
                    logger.error { e.localizedMessage }
                    return null
                }
            } else {
                val bytesArray = SerializationUtils.serialize(content as String)
                return Base64.getEncoder().encodeToString(bytesArray)
            }
        }

        /**
         * Creates a dead-letter report if there is a malformed JSON. This is called if DISABLE_VALIDATION is true
         *
         * @param deadLetterReason String
         * @return String
         * @throws BadStateException
         */
        @Throws(BadStateException::class)
        fun createDeadLetterReport(deadLetterReason: String): String {
            val deadLetterReportId = UUID.randomUUID().toString()
            val deadLetterReport = ReportDeadLetter().apply {
                this.id = deadLetterReportId
                this.deadLetterReasons = listOf(deadLetterReason)
                this.source= source.toString()
            }
            return createReportItem(null, deadLetterReportId, deadLetterReport)
        }


        /**
         * The common function which writes to cosmos container based on the report type
         *
         * @param uploadId String?
         * @param reportId String
         * @param reportType Any
         * @return String - report identifier
         */
        private fun createReportItem(uploadId: String?, reportId: String, reportType: Any): String {
            var success = false

            // Determine whether report type is Report or ReportDeadLetter
            when (reportType) {
                is ReportDeadLetter -> {
                    success = repository.reportsDeadLetterCollection.createItem(
                        reportId,
                        reportType,
                        ReportDeadLetter::class.java,
                        uploadId
                    )
                }

                is Report -> {
                    success = repository.reportsCollection.createItem(
                        reportId,
                        reportType,
                        Report::class.java,
                        uploadId
                    )
                }
            }

            if (success)
                return reportId

            throw BadStateException("Failed to create ${reportType::class.simpleName} with reportId = reportId, uploadId = $uploadId")
        }
    }