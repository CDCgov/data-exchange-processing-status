package gov.cdc.ocio.processingstatusapi.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.ocio.processingstatusapi.ReportManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage

fun isJsonValid(jsonString: String): Boolean {
    return try {
        val mapper = jacksonObjectMapper()
        mapper.readTree(jsonString)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Checks for depreciated fields within message that are still accepted for backward compatability.
 * If any depreciated fields are found, they are replaces with their corresponding new fields.
 */
fun checkAndReplaceDeprecatedFields(messageAsString: String): String {
    var message = messageAsString
    if (message.contains("destination_id")) {
        message= message.replace("destination_id", "data_stream_id")
    }
    if (message.contains("event_type")) {
        message = message.replace("event_type", "data_stream_route")
    }
    return message
}
fun sendToDeadLetter(reason:String){
    //This should not run for unit tests
    if (System.getProperty("isTestEnvironment") != "true") {
        // Write the content of the dead-letter reports to CosmosDb
        ReportManager().createDeadLetterReport(reason)
        throw BadRequestException(reason)
    }
}
fun sendToDeadLetter(invalidData:MutableList<String>, validationSchemaFileNames:MutableList<String>, createReportMessage: CreateReportSBMessage){
    if (invalidData.isNotEmpty()) {
        //This should not run for unit tests
        if (System.getProperty("isTestEnvironment") != "true") {
            // Write the content of the dead-letter reports to CosmosDb
            ReportManager().createDeadLetterReport(
                createReportMessage.uploadId,
                createReportMessage.dataStreamId,
                createReportMessage.dataStreamRoute,
                createReportMessage.messageMetadata,
                createReportMessage.stageInfo,
                createReportMessage.tags,
                createReportMessage.data,
                createReportMessage.dispositionType,
                createReportMessage.contentType,
                createReportMessage.content,
                createReportMessage.jurisdiction,
                createReportMessage.senderId,
                invalidData,
                validationSchemaFileNames
            )
        }
        throw BadRequestException(invalidData.joinToString(separator = ","))
    }
}