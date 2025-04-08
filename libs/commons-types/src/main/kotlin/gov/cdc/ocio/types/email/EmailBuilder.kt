package gov.cdc.ocio.types.email

import kotlinx.html.*
import kotlinx.html.stream.appendHTML


/**
 * Convenience class for building HTML emails.  Emails are dispatched by the notifications systems, namely the
 * notifications rules engine and notification workflow services.
 *
 * @property includeCommonHeader Boolean
 * @property htmlBody String
 */
class EmailBuilder {

    private var includeCommonHeader = false

    private var htmlBody = ""

    /**
     * Include common HTML styles used by HTML base emails.
     *
     * @param includeCommonHeader Boolean
     * @return EmailBuilder
     */
    fun commonHeader(includeCommonHeader: Boolean) = apply {
        this.includeCommonHeader = includeCommonHeader
    }

    /**
     * Used to specify the HTML body.
     *
     * @param htmlBody String
     * @return EmailBuilder
     */
    fun htmlBody(htmlBody: String) = apply {
        this.htmlBody = htmlBody
    }

    /**
     * Build the header, mainly 'styles' to include all the common styles used in PHDO emails.
     *
     * @return String
     */
    private fun header(): String {
        return buildString {
            appendHTML().html {
                head {
                    style {
                        +"""
                            body {
                                font-family: Arial, sans-serif;
                                margin: 20px;
                            }
                            table {
                                border-collapse: collapse;
                            }
                            td {
                                padding: 4px 0;
                            }
                            td:first-child {
                                padding-right: 20px;
                            }
                            .json-container {
                                background: #ededed;
                                color: #000000;
                                padding: 15px;
                                border-radius: 8px;
                                overflow-x: auto;
                                font-family: 'Courier New', monospace;
                                white-space: pre-wrap;
                                word-wrap: break-word;
                            }
                            .bold-uppercase {
                                font-weight: bold;
                                text-transform: uppercase;
                            }
                            .uppercase {
                                text-transform: uppercase;
                            }
                        """.trimIndent()
                    }
                }
            }
        }
    }

    fun build(): String = buildString {
        if (includeCommonHeader) append(header())
        append(htmlBody)
    }

}