package gov.cdc.ocio.message

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

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
        val factory: JsonFactory = JsonFactory();
        val mapper: ObjectMapper = ObjectMapper(factory);
        val rootNode: JsonNode = mapper.readTree(content);
        val fieldsIterator: Iterator<Map.Entry<String,JsonNode>>  = rootNode.fields();
        while (fieldsIterator.hasNext()) {

            val field: Map.Entry<String,JsonNode>  = fieldsIterator.next();
            System.out.println("Key: " + field.key + "\tValue:" + field.value);
        }
    }
}