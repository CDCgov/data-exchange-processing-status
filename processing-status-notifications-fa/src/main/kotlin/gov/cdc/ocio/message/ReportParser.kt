package gov.cdc.ocio.message

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.exceptions.ContentException
import kotlin.collections.HashMap

/**
 * Class containing util methods to parse the content of the report
 *
 */
class ReportParser {

    private val reportTypeToStatusFieldMapping: MutableMap<String, String> = mutableMapOf("dex-hl7-validation" to "status",
                                            "dex-file-copy" to "result",
                                            "dex-metadata-verify" to "issues",
                                            "upload" to "abc" // TODO : find this
        )

    /**
     * Method which parses different types of notification reports for report status
     * @param content String
     * @return Unit
     */
    fun parseReportForStatus(content: String, reportType: String?): String {
        val reportMetricCollector = HashMap<String, String>()
        val factory = JsonFactory()
        val mapper = ObjectMapper(factory)
        val rootNode: JsonNode = mapper.readTree(content)

        recurseParseHelper(rootNode, reportMetricCollector, reportType!!)
        return reportMetricCollector[reportTypeToStatusFieldMapping[reportType]]!!
    }

    @Throws(ContentException::class)
    private fun recurseParseHelper(node: JsonNode, reportMetricMap: HashMap<String, String>, reportType: String) {
        if (node.isNull) {
            return
        }

        val statusFieldName = reportTypeToStatusFieldMapping[reportType]
        val fieldsIterator: Iterator<Map.Entry<String,JsonNode>>  = node.fields()
        while (fieldsIterator.hasNext()) {
            val field: Map.Entry<String,JsonNode>  = fieldsIterator.next()
            if (field.value.isArray) {
                // Specifically for Metadata Report type to figure out the status from issues fields
                if (field.key.equals(statusFieldName, true) && statusFieldName == "issues") {
                    if (!field.value.isArray) {
                        throw ContentException("Invalid content in $reportType")
                    } else {
                        val status = if (field.value.size() > 0) "failure" else "success"
                        reportMetricMap[statusFieldName] = status
                        return
                    }
                }

                for(element in field.value) {
                    recurseParseHelper(element, reportMetricMap, reportType)
                }
            } else if (field.value.isObject){
               recurseParseHelper(field.value, reportMetricMap, reportType)
            } else {
                if (field.key.equals(statusFieldName, true)) {
                    processStatusValueInArray(field, reportMetricMap, reportType)
                }
           }

        }
    }
    private fun processStatusValueInArray(field: Map.Entry<String,JsonNode>, reportMetricMap: HashMap<String, String>, statusFieldName: String) {
        if (!reportMetricMap.containsKey(statusFieldName)) {
            reportMetricMap[statusFieldName] = field.value.textValue()
        } else {
            val existingStatus = getPrecedenceOfStatus(reportMetricMap.get(statusFieldName))
            val newStatus = getPrecedenceOfStatus(field.value.textValue())
            if (existingStatus < newStatus) {
                reportMetricMap[statusFieldName] = field.value.textValue()
            }
        }
    }
    private fun getPrecedenceOfStatus(status: String?): Int {
        if (status != null) {
            return when (status.lowercase()) {
                "success" -> 1
                "warning" -> 2
                "failure" -> 3
                else -> 0
            }
        }
        return 0
    }
}