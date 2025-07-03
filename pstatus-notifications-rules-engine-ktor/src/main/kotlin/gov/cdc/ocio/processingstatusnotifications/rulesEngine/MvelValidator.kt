package gov.cdc.ocio.processingstatusnotifications.rulesEngine

import gov.cdc.ocio.types.model.MessageMetadata
import gov.cdc.ocio.types.model.StageInfo
import gov.cdc.ocio.types.model.Status
import mu.KotlinLogging
import org.mvel2.MVEL
import org.mvel2.ParserContext

/**
 * A utility class for validating MVEL expressions in a strongly-typed context. This class enables safe evaluation of
 * MVEL syntax by ensuring that the expressions conform to the expected structure and use of variables.
 *
 * @property contextVars A map defining the context variables available in the expression, where the key represents
 * the variable name and the value is its corresponding class type.
 *
 * The default context includes:
 * - stageInfo: Represents metadata related to a processing stage using the StageInfo class.
 * - messageMetadata: Represents metadata of a message using the MessageMetadata class.
 * - Status: A reference to the Status enum indicating processing status.
 *
 * @constructor Initializes the MvelValidator with a map of context variables. If no context is provided, the default
 * context is used.
 */
class MvelValidator(
    private val contextVars: Map<String, Class<*>> = defaultContext
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Creates a strongly-typed parser context for validating MVEL expressions. The parser context is initialized
     * with strong typing enabled and populated with the defined context variables from the containing class.
     *
     * @return A configured instance of ParserContext with strong typing and registered context variables.
     */
    private fun createParserContext(): ParserContext {
        val context = ParserContext()
        context.isStrongTyping = true
        contextVars.forEach { (name, clazz) ->
            context.addInput(name, clazz)
        }
        return context
    }

    /**
     * Validates the given MVEL expression for syntax and context correctness.
     *
     * @param expression The MVEL expression to be validated as a string.
     * @return An instance of MvelValidationResult indicating whether the validation was successful
     *         and providing an error message if applicable.
     */
    fun validate(expression: String): MvelValidationResult {
        return try {
            val parserContext = createParserContext()
            val cleanedExpression = MvelExpressionCleaner.removeClauses(expression)
            logger.info("Cleaned expression: $cleanedExpression")
            MVEL.analysisCompile(cleanedExpression, parserContext)
            MvelValidationResult(true)
        } catch (ex: Exception) {
            MvelValidationResult(false, ex.message)
        }
    }

    companion object {
        val defaultContext = mapOf(
            "stageInfo" to StageInfo::class.java,
            "messageMetadata" to MessageMetadata::class.java,
            "Status" to Status::class.java,
        )
    }
}