package email

import gov.cdc.ocio.types.email.EmailBuilder
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


class EmailBuilderTest {

    @Test
    fun `build email`() {
        val htmlBody =
            createHTML().body {
                h3 { +"I love testing" }
            }

        val htmlString = EmailBuilder()
            .commonHeader(true)
            .htmlBody(htmlBody)
            .build()

        val document = Jsoup.parse(htmlString)

        // Ensure the document is not null
        assertNotNull(document)

        // Check if required elements exist
        assertNotNull(document.selectFirst("html"), "Missing <html> tag")
        assertNotNull(document.selectFirst("head"), "Missing <head> tag")
        assertNotNull(document.selectFirst("body"), "Missing <body> tag")

        // Validate content
        assertEquals(document.selectFirst("h3")?.text(), "I love testing", "H3 content mismatch")
    }

}