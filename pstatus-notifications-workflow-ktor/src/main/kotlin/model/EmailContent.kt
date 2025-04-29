package gov.cdc.ocio.processingnotifications.model

import kotlinx.html.*

fun BODY.workflowHeader() {
    div {
        span(classes = "bold-uppercase") { +"\u271A Public Health" }
        span(classes = "uppercase") { +" Data Operations" }
    }
    hr {  }
}

/**
 * Html BODY extension for the common workflow footer.
 *
 * @receiver BODY
 */
fun BODY.workflowFooter() {
    div {
        br {  }
        br {  }
        hr { }
        small {
            +("Subscriptions to this email are managed by the Public Health Data Operations (PHDO) "
                    + "Processing Status (PS) API. Use the PS API GraphQL interface to unsubscribe "
                    + "with the workflow ID provided above. ")
            a(href = "https://cdcgov.github.io/data-exchange/") { +"Click here" }
            + " for more information."
        }
    }
}
