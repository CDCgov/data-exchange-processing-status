import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.support.reader.JsonRuleDefinitionReader
import org.jeasy.rules.support.reader.RuleDefinitionReader
import org.jeasy.rules.mvel.MVELRuleFactory
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader
import java.io.File
import java.io.FileReader
import java.time.LocalTime

fun main() {

    if(System.getenv("RULE_PROCESSING_TYPE")!= "yml")
         jsonRuleProcessor()
     else
         yamlRuleProcessor()
}

private fun jsonRuleProcessor(){

    // Load rules from JSON
    val ruleDefinitionReader: RuleDefinitionReader = JsonRuleDefinitionReader()
    val ruleFactory = MVELRuleFactory(ruleDefinitionReader)
    val jsonFilePath = object {}.javaClass.getResource("/json/rules.json")
    val file = File(jsonFilePath!!.toURI())
    val ruleDefinitions = ruleFactory.createRules(FileReader(file))
    // Create a Rules object with the loaded rules
    val rules = Rules(ruleDefinitions.toSet())

    // Create facts
    val facts = Facts()
    facts.put("Jurisdiction", Jurisdiction( "TestJurisdiction", LocalTime.of(12, 10)))
    facts.put("UploadStatus", UploadStatus(true, true))
    facts.put("dataStreams", listOf(DataStream("Stream1", 10), DataStream("Stream2", 20)))

    // Create rules engine
    val rulesEngine = DefaultRulesEngine()
    // Fire rules
    rulesEngine.fire(rules, facts)
}

private fun yamlRuleProcessor(){

    // Load rules from JSON
    val ruleDefinitionReader = YamlRuleDefinitionReader()
    val ruleFactory = MVELRuleFactory(ruleDefinitionReader)
    val ymlFilePath = object {}.javaClass.getResource("/yaml/rules.yml")
    val file = File(ymlFilePath!!.toURI())
    val ruleDefinitions = ruleFactory.createRules(FileReader(file))
    // Create a Rules object with the loaded rules
    val rules = Rules(ruleDefinitions.toSet())

    // Create facts
    val facts = Facts()
    facts.put("Jurisdiction", Jurisdiction( "TestJurisdiction", LocalTime.of(12, 10)))
    facts.put("UploadStatus", UploadStatus(true, true))
    facts.put("dataStreams", listOf(DataStream("Stream1", 10), DataStream("Stream2", 20)))

    // Create rules engine
    val rulesEngine = DefaultRulesEngine()
    // Fire rules
    rulesEngine.fire(rules, facts)
}