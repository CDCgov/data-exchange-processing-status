package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import mu.KotlinLogging
import org.knowm.xchart.*
import java.awt.Color
import java.awt.Font
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.pow


/**
 * Delivery latency histogram chart.
 *
 * @property logger KLogger
 * @property chart [CategoryChart]
 * @constructor
 */
class DeliveryLatencyChart(
    deliveryLatenciesInMillis: List<Long>,
    width: Int,
    height: Int
) {
    private val logger = KotlinLogging.logger {}

    private val chart = CategoryChartBuilder()
        .width(width)  // Set chart width
        .height(height) // Set chart height
        .title("Duration Distribution")
        .xAxisTitle("Duration Bins (seconds)")
        .yAxisTitle("Number of Uploads")
        .build()

    init {
        // Compute Inter-quartile Range (IQR). IQR in a histogram is a measure of statistical dispersion, representing
        // the range within which the middle 50% of data points fall. A narrow IQR suggests that most data points are
        // closely packed. A wide IQR indicates greater variability in the data.
        val sortedValues = deliveryLatenciesInMillis.sorted()
        val durationValuesInSeconds = sortedValues.map { it / 1000.0 }
        val q1 = durationValuesInSeconds[durationValuesInSeconds.size / 4]
        val q3 = durationValuesInSeconds[3 * durationValuesInSeconds.size / 4]
        val iqr = q3 - q1

        // Freedman-Diaconis Rule
        val binWidth = 2 * iqr / durationValuesInSeconds.size.toDouble().pow(1.0 / 3.0)
        val numBins = ((durationValuesInSeconds.maxOrNull()!! - durationValuesInSeconds.minOrNull()!!) / binWidth)
            .toInt()
            .coerceAtLeast(1)

        // Create the histogram data series
        val histogramData = Histogram(durationValuesInSeconds, numBins)
        val series = chart.addSeries("Latency", histogramData.getxAxisData(), histogramData.getyAxisData())
        series.setFillColor(Color(40, 100, 160)) // Red bars

        // Customize the chart's styling for a modern look
        chart.styler.apply {
            isLegendVisible = false
            // Set the chart background color
            chartBackgroundColor = Color(255, 255, 255) // White background
            plotBackgroundColor = Color(255, 255, 255) // White plot area
            plotGridLinesColor = Color(220, 220, 220) // Light grey grid lines

            // Style axis labels
            axisTickMarksColor = Color(150, 150, 150) // Subtle axis tick marks
            axisTitleFont = Font("Arial", Font.PLAIN, 14) // Clean, modern font for axis titles
            axisTickLabelsFont = Font("Arial", Font.PLAIN, 12) // Modern font for axis labels

            // Style chart title
            chartTitleFont = Font("Arial", Font.BOLD, 18) // Bold, larger font for title

            xAxisDecimalPattern = "#.0" // Limit x-axis labels to 1 decimal place
        }
    }

    /**
     * Convert this chart into a PNG image byte array.
     *
     * @return ByteArray
     */
    fun toPngAsByteArray(): ByteArray {
        // Save chart to PNG in memory (ByteArrayOutputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()
        BitmapEncoder.getBufferedImage(chart).also {
            ImageIO.write(it, "PNG", byteArrayOutputStream)
        }
        val chartInBytes = byteArrayOutputStream.toByteArray()
        return chartInBytes
    }
}
