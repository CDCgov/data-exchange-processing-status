class EmailNotificationService {

    private val subscribedEmails = mutableSetOf<String>()

    fun subscribe(email: String): SubscriptionResponse {
        return if (subscribedEmails.add(email)) {
            SubscriptionResponse(true, "Successfully subscribed to email notifications.")
        } else {
            SubscriptionResponse(false, "Email is already subscribed.")
        }
    }

    fun unsubscribe(email: String): UnsubscriptionResponse {
        return if (subscribedEmails.remove(email)) {
            UnsubscriptionResponse(true, "Successfully unsubscribed from email notifications.")
        } else {
            UnsubscriptionResponse(false, "Email is not subscribed.")
        }
    }
}
