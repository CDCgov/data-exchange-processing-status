class NotificationActivitiesImpl : NotificationActivities {
    override fun sendNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        deliveryReference: String
    ) {
        println("Sending the notification to $deliveryReference for dataStreamId: $dataStreamId, jurisdiction: $jurisdiction")
    }
}
