package gov.cdc.ocio.types.model

import com.fasterxml.jackson.annotation.JsonFormat
import gov.cdc.ocio.types.serializers.LocalTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalTime


/**
 * Represents a workflow subscription for checking deadlines in a specific data stream. This class extends
 * WorkflowSubscription and adds additional properties specific to handling deadlines. This includes properties
 * for identifying the data stream, expected jurisdictions, and the time of the deadline.
 *
 * @property dataStreamId A unique identifier for the data stream associated with this subscription.
 * @property dataStreamRoute Specifies the route or path for the data stream. Used for mapping the data stream workflow.
 * @property expectedJurisdictions A list of jurisdictions that are expected to be relevant for this workflow.
 * @property deadlineTime Specifies the time of the deadline that this subscription monitors, serialized using a custom
 * serializer for LocalTime.
 */
@Serializable
data class WorkflowSubscriptionDeadlineCheck(
    override val cronSchedule: String,
    override val notificationType: NotificationType,
    override val emailAddresses: List<String>?,
    override val webhookUrl: String?,
    val dataStreamId: String,
    val dataStreamRoute: String,
    val expectedJurisdictions: List<String>,
    @Serializable(with = LocalTimeSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    val deadlineTime: LocalTime
) : WorkflowSubscription()