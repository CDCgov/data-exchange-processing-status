package gov.cdc.ocio.types.model

import gov.cdc.ocio.types.serializers.LocalTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class WorkflowSubscriptionDeadlineCheck(
    override val dataStreamIds: List<String>,
    override val dataStreamRoutes: List<String>,
    override val jurisdictions: List<String>,
    override val cronSchedule: String,
    override val notificationType: NotificationType,
    override val emailAddresses: List<String>?,
    override val webhookUrl: String?,
    @Serializable(with = LocalTimeSerializer::class)
    val deadlineTime: LocalTime
) : WorkflowSubscription()