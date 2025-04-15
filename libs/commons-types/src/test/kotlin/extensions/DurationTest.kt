package extensions

import gov.cdc.ocio.types.extensions.toHumanReadable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationTest {

    @Test
    fun `duration 2400 millis`() {
        val duration = Duration.ofMillis(2400)

        // Validate content
        assertEquals(duration.toHumanReadable(), "2.400 sec")
    }

    @Test
    fun `duration hours, minutes, and seconds`() {
        val duration = Duration.ofHours(2)
            .plusMinutes(32)
            .plusSeconds(26)
            .plusMillis(5)

        // Validate content
        assertEquals(duration.toHumanReadable(), "2 hr 32 min 26.005 sec")
    }

    @Test
    fun `duration hours only`() {
        val duration = Duration.ofHours(5)

        // Validate content
        assertEquals(duration.toHumanReadable(), "5 hr")
    }

    @Test
    fun `duration hours and minutes`() {
        val duration = Duration.ofHours(3)
            .plusMinutes(2)

        // Validate content
        assertEquals(duration.toHumanReadable(), "3 hr 2 min")
    }
}