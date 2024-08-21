class EmailNotificationResolver(private val emailNotificationService: EmailNotificationService) {

    fun subscribeToEmail(email: String): SubscriptionResponse {
        return emailNotificationService.subscribe(email)
    }

    fun unsubscribeFromEmail(email: String): UnsubscriptionResponse {
        return emailNotificationService.unsubscribe(email)
    }
}
