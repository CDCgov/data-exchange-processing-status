package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.notificationdispatchers.NotificationDispatcher
import gov.cdc.ocio.notificationdispatchers.model.LoggerNotificationContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class LoggerNotificationAction : NotificationAction, KoinComponent {

    private val dispatchWorker by inject<NotificationDispatcher>()

    /**
     * For logging notifications the content can be [Any].
     *
     * @param payload Any
     */
    override fun doNotify(payload: Any) {
        dispatchWorker.send(LoggerNotificationContent(content = payload))
    }
}
