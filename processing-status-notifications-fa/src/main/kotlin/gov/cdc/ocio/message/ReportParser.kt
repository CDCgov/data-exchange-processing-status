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

    /**
     * Method which parses different types of notification reports for various fields like 'StatusType'
     * @param content String
     * @return Unit
     */
    fun parseReport(content: String): Unit {
        val reportMetricMap = HashMap<String, String>();
        val factory: JsonFactory = JsonFactory();
        val mapper: ObjectMapper = ObjectMapper(factory);
        val rootNode: JsonNode = mapper.readTree(content);
       parseReportHelper(rootNode, reportMetricMap)

    }

    fun parseReportHelper(node: JsonNode, reportMetricMap: HashMap<String, String>) {
        if (node.isNull) {
            return
        }

        val fieldsIterator: Iterator<Map.Entry<String,JsonNode>>  = node.fields();
        while (fieldsIterator.hasNext()) {
            val field: Map.Entry<String,JsonNode>  = fieldsIterator.next();
           if (field.value.isArray) {
                for(element in field.value) {
                    parseReportHelper(element, reportMetricMap)
                }
            } else if (field.value.isObject){
                parseReportHelper(field.value, reportMetricMap)
            } else {
                if (field.key.contains("status")) {
                    val metric = field.key
                    if (!reportMetricMap.containsKey(metric)) {
                        reportMetricMap.put(metric, field.value.textValue())
                    }
                }
               System.out.println(field.key + " : " + field.value);
           }

        }
    }
}