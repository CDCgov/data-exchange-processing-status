package gov.cdc.ocio.processingstatusnotifications.EasyRules

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

class RuleLoader {
    private val jsonMapper = ObjectMapper()
    private val yamlMapper = ObjectMapper(YAMLFactory())

    fun loadRulesFromJson(filePath: String): List<Rule> {
        return jsonMapper.readValue(File(filePath), jsonMapper.typeFactory.constructCollectionType(List::class.java, Rule::class.java))
    }

    fun loadRulesFromYaml(filePath: String): List<Rule> {
        return yamlMapper.readValue(File(filePath), yamlMapper.typeFactory.constructCollectionType(List::class.java, Rule::class.java))
    }
}
