package gov.cdc.ocio.message

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
    fun parseReportForStatus(content: String, fileType: String?): String {
        val reportMetricCollector = HashMap<String, String>()
        val factory = JsonFactory();
        val mapper = ObjectMapper(factory);
        val rootNode: JsonNode = mapper.readTree(content);
        val statusFieldName = reportTypeToStatusFieldMapping[fileType]
        recurseParseHelper(rootNode, reportMetricCollector, statusFieldName!!)
        return reportMetricCollector.get(statusFieldName)!!;
    }

    private fun recurseParseHelper(node: JsonNode, reportMetricMap: HashMap<String, String>, statusFieldName: String) {
        if (node.isNull) {
            return
        }

        val fieldsIterator: Iterator<Map.Entry<String,JsonNode>>  = node.fields();
        while (fieldsIterator.hasNext()) {
            val field: Map.Entry<String,JsonNode>  = fieldsIterator.next();
           if (field.value.isArray) {
                for(element in field.value) {
                    recurseParseHelper(element, reportMetricMap, statusFieldName)
                }
            } else if (field.value.isObject){
               recurseParseHelper(field.value, reportMetricMap, statusFieldName)
            } else {
                if (field.key.equals(statusFieldName, true)) {
                    processStatusValueInArray(field, reportMetricMap, statusFieldName)
                }
           }

        }
    }

    private fun processStatusValueInArray(field: Map.Entry<String,JsonNode>, reportMetricMap: HashMap<String, String>, statusFieldName: String) {
        if (!reportMetricMap.containsKey(statusFieldName)) {
            reportMetricMap.put(statusFieldName, field.value.textValue())
        } else {
            val existingStatus = getPrecedenceOfStatus(reportMetricMap.get(statusFieldName))
            val newStatus = getPrecedenceOfStatus(field.value.textValue())
            if (existingStatus < newStatus) {
                reportMetricMap.put(statusFieldName, field.value.textValue())
            }
        }
    }

    fun getPrecedenceOfStatus(status: String?): Int {
        if (status != null) {
            return when (status.lowercase()) {
                "success" -> 1
                "warning" -> 2
                "failure" -> 3
                else -> 0
            }
        }
        return 0;
    }
}