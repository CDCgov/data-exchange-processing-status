package gov.cdc.ocio.processingstatusnotifications.rulesEngine

/**
 * An object for parsing, tokenizing, and cleaning MVEL expressions.
 * Its primary purpose is to remove clauses containing "content." and reconstruct the expression
 * appropriately, while respecting the correct usage of logical operators and parentheses.
 */
object MvelExpressionCleaner {

    // Helper to check if a token is a boolean operator
    private fun isOperator(token: String) = token == "&&" || token == "||"

    /**
     * Tokenizes an MVEL expression, respecting parentheses and top-level logical operators.
     * Tokens can be clauses, operators (&&, ||), or parenthesized sub-expressions.
     *
     * @param expr The logical expression to tokenize as a string.
     * @return A list of string tokens derived from the input expression.
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
     * Recursively strips clauses containing "content." from a list of tokens.
     * It rebuilds the expression with correct operator placement.
     *
     * @param tokens A list of token strings representing parts of an expression.
     *               Tokens may represent clauses, operators, or parenthesized groups.
     * @return A reconstructed string expression with unwanted parts removed,
     *         redundant operators cleaned up, and valid clauses retained. If all tokens are removed,
     *         returns an empty string.
     */
    private fun stripContentPartsAndReconstruct(tokens: List<String>): String {
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
                    val cleanedInner = removeContentClauses(inner).trim()
                    if (cleanedInner.isNotBlank()) {
                        // If inner becomes empty, remove the whole parenthesis.
                        // If it becomes "true", keep "(true)" as it's valid MVEL.
                        filteredTokens.add("($cleanedInner)")
                    }
                }
                // If it's a clause containing "content.", skip it
                token.contains("content.", ignoreCase = true) -> {
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
     * Removes clauses containing "content." from the given MVEL expression and reconstructs the cleaned expression.
     * If the resulting expression becomes empty and originally contained "content." clauses,
     * the function defaults to returning "true".
     *
     * @param expression The MVEL expression as a string, which may contain clauses, logical operators, and parentheses.
     * @return The cleaned expression string with all "content." clauses removed, or "true" if the resulting expression
     *         is empty and originally contained "content.".
     */
    fun removeContentClauses(expression: String): String {
        val tokens = tokenize(expression)
        val cleanedExpression = stripContentPartsAndReconstruct(tokens)

        // If the expression becomes empty after cleaning and it originally contained "content.",
        // you might want to default it to "true" or "" based on your application's logic.
        // Returning "true" is common if the expression is for a conditional check.
        return if (cleanedExpression.isBlank() && expression.contains("content.", ignoreCase = true)) {
            "true"
        } else {
            cleanedExpression
        }
    }
}