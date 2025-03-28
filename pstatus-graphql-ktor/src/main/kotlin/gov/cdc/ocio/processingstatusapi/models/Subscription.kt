package gov.cdc.ocio.processingstatusapi.models

import gov.cdc.ocio.types.adapters.NotificationTypeAdapter
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.types.model.Notification
import gov.cdc.ocio.types.model.SubscriptionRule


/**
 * Notification subscription definition for the rules engine service.
 *
 * @property subscriptionId String
 * @property subscriptionRule SubscriptionRule
 * @property notification Notification
 * @constructor
 */
data class Subscription(
    val subscriptionId: String,
    val subscriptionRule: SubscriptionRule,
    val notification: Notification
) {
    companion object {
        private val gson = GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Notification::class.java, NotificationTypeAdapter())
            .create()

        private val listType = object : TypeToken<List<Subscription>>() {}.type

        /**
         * Deserialize the provided json string into a list of subscriptions.
         *
         * @param json String
         * @return List<Subscription>
         */
        fun fromJson(json: String): List<Subscription> {
            return gson.fromJson(json, listType)
        }
    }
}