package gov.cdc.ocio.processingstatusnotifications.rulesEngine

/**
 * An object for parsing, tokenizing, and cleaning MVEL expressions.
 * Its primary purpose is to remove clauses containing "content." and reconstruct the expression
 * appropriately, while respecting the correct usage of logical operators and parentheses.
 */
import java.util.regex.Pattern

object MvelExpressionCleaner {

    private fun isOperator(token: String) = token == "&&" || token == "||"

    /**
     * Tokenizes an MVEL expression, respecting parentheses and top-level logical operators.
     * Tokens can be clauses, operators (&&, ||), or parenthesized sub-expressions.
     *
     * @param expr String
     * @return List<String>
     */
    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        var parens = 0 // Tracks nesting level of parentheses
        var current = StringBuilder()

        while (i < expr.length) {
            val c = expr[i]

            when {
                // Handle opening parenthesis
                c == '(' -> {
                    if (parens == 0 && current.isNotEmpty()) { // If top-level content before '('
                        tokens.add(current.toString().trim())
                        current = StringBuilder()
                    }
                    current.append(c)
                    parens++
                }
                // Handle closing parenthesis
                c == ')' -> {
                    current.append(c)
                    parens--
                    if (parens == 0) { // End of top-level parenthesized group
                        tokens.add(current.toString().trim())
                        current = StringBuilder()
                    }
                }
                // Handle top-level '&&' operator
                parens == 0 && expr.startsWith("&&", i) -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString().trim())
                        current = StringBuilder()
                    }
                    tokens.add("&&")
                    i++ // Consume the second '&'
                }
                // Handle top-level '||' operator
                parens == 0 && expr.startsWith("||", i) -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString().trim())
                        current = StringBuilder()
                    }
                    tokens.add("||")
                    i++ // Consume the second '|'
                }
                // Append character to current token
                else -> {
                    current.append(c)
                }
            }
            i++ // Move to the next character
        }

        // Add any remaining part of the expression as a token
        if (current.isNotEmpty()) {
            tokens.add(current.toString().trim())
        }

        // Filter out any blank tokens that might result from trimming/whitespace
        return tokens.filter { it.isNotBlank() }
    }

    /**
     * Checks if a clause string contains the specified field name using dot or bracket notation.
     * Examples: "fieldName.property", "fieldName['property']", "fieldName[0]"
     *
     * @param clause String
     * @param fieldName String
     * @return Boolean
     */
    private fun doesClauseContainField(
        clause: String,
        fieldName: String
    ): Boolean {
        val pattern = Pattern.compile(
            "\\b$fieldName\\s*\\.\\s*|\\b$fieldName\\s*\\[", // Matches "fieldName." or "fieldName ["
            Pattern.CASE_INSENSITIVE
        )
        return pattern.matcher(clause).find()
    }

    /**
     * Recursively strips clauses containing the specified field name from a list of tokens.
     * It rebuilds the expression with correct operator placement.
     *
     * @param tokens List<String>
     * @param fieldNameToRemove String
     * @return String
     */
    private fun stripContentPartsAndReconstruct(
        tokens: List<String>,
        fieldNameToRemove: String
    ): String {
        if (tokens.isEmpty()) {
            return ""
        }

        val filteredTokens = mutableListOf<String>()
        for (i in tokens.indices) {
            val token = tokens[i]

            when {
                // If it's a parenthesized group, recurse and add the cleaned inner part
                token.startsWith("(") && token.endsWith(")") -> {
                    val inner = token.substring(1, token.length - 1)
                    // Recursive call needs to pass the field name
                    val cleanedInner = removeClauses(inner, fieldNameToRemove).trim()
                    if (cleanedInner.isNotBlank()) {
                        // If inner becomes empty, remove the whole parenthesis.
                        // If it becomes "true", keep "(true)" as it's valid MVEL.
                        filteredTokens.add("($cleanedInner)")
                    }
                }
                // If it's a clause containing the specified field, skip it
                doesClauseContainField(token, fieldNameToRemove) -> {
                    // Do nothing, effectively removing this token
                }
                // If it's an operator, add it. Operators will be cleaned up in reconstruction.
                isOperator(token) -> {
                    filteredTokens.add(token)
                }
                // Otherwise, it's a valid clause to keep
                else -> {
                    filteredTokens.add(token)
                }
            }
        }

        // --- Reconstruction Phase ---
        val finalExpressionParts = mutableListOf<String>()
        var lastWasClause = false

        for (token in filteredTokens) {
            if (isOperator(token)) {
                if (lastWasClause) { // Add operator only if there's a clause before it
                    finalExpressionParts.add(token)
                    lastWasClause = false
                }
            } else {
                finalExpressionParts.add(token)
                lastWasClause = true
            }
        }

        // Join parts and perform final cleanup of operators and parentheses
        var result = finalExpressionParts.joinToString(" ").trim()

        // Remove redundant operators (e.g., "A && && B" -> "A && B")
        result = result.replace("\\s*&&\\s*&&\\s*".toRegex(), " && ")
            .replace("\\s*\\|\\|\\s*\\|\\|\\s*".toRegex(), " || ")

        // Remove leading/trailing operators if the expression starts/ends with one
        if (result.startsWith("&&") || result.startsWith("||")) {
            val firstNonOpIndex = result.indexOfFirst { !it.isWhitespace() && it != '&' && it != '|' }
            result = if (firstNonOpIndex != -1) result.substring(firstNonOpIndex).trim() else ""
        }
        if (result.endsWith("&&") || result.endsWith("||")) {
            val lastNonOpIndex = result.indexOfLast { !it.isWhitespace() && it != '&' && it != '|' }
            result = if (lastNonOpIndex != -1) result.substring(0, lastNonOpIndex + 1).trim() else ""
        }

        // Remove empty parentheses that might result from content removal like "A && ()"
        result = result.replace("\\(\\s*\\)".toRegex(), "").trim()

        return result
    }

    /**
     * Main function to remove clauses containing the specified field name from an MVEL expression.
     *
     * @param expression The MVEL expression string.
     * @param fieldNameToRemove The name of the field to look for (e.g., "content", "data").
     * Defaults to "content".
     * @return The modified expression string with clauses containing the field removed.
     */
    fun removeClauses(
        expression: String,
        fieldNameToRemove: String = "content"
    ): String {
        val tokens = tokenize(expression)
        val cleanedExpression = stripContentPartsAndReconstruct(tokens, fieldNameToRemove)

        // If the expression becomes empty after cleaning and it originally contained the field,
        // default it to "true" (common for boolean expressions) or "" based on your logic.
        // Use the general fieldNameToRemove to check for original presence.
        return if (cleanedExpression.isBlank() && doesClauseContainField(expression, fieldNameToRemove)) {
            "true"
        } else {
            cleanedExpression
        }
    }
}