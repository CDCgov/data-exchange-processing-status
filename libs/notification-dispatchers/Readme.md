# notifications-dispatcher
The `notifications-dispatcher` library provides an interface for dispatching notifications for various systems, including emails and webhooks.

### gradle
Add the following to the `dependencies` of your project' `build.gradle`.
```groovy
dependencies {
    implementation project(':libs:notifications-dispatcher')
}
```
This will allow the `notifications-dispatcher` to be compiled if necessary and linked with your project.  You can set breakpoints in the library the same as you would your main project for debugging.

### Email Notification Setup
In the `application.conf` you will need to specify the email dispatcher type. Use environment variable
`EMAIL_PROTOCOL` to indicate which type to use. The supported types are:
- `SMTP` - Uses Standard Mail Transfer Protocol to send emails.
When adding support for the notifications dispatcher to your ktor application, you will need to specify the email dispatcher in the `ktor` section of `application.conf`.
```
ktor {
    emailProtocol = "SMTP" # default to SMTP if EMAIL_PROTOCOL is missing
    emailProtocol = ${?EMAIL_PROTOCOL}
}
```

#### SMTP Email Dispatcher Setup
Add the following section to your `application.conf`.
```
smtp {
    host = ${?SMTP_HOST}
    port = ${?SMTP_PORT}
    auth = ${?SMTP_AUTH}
    username = ${?SMTP_USERNAME}
    password = ${?SMTP_PASSWORD}
}
```
Set your environment variables as follows. 
- `SMTP_HOST` - hostname of the SMTP server
- `SMTP_PORT` - port number to use, typically 25
- `SMTP_AUTH` - if true, then the `username` and `password` is used to authenticate with the SMTP server
- `SMTP_USERNAME` - username used for SMTP server auth
- `SMTP_PASSWORD` - password used for SMTP server auth
> NOTE: If you do not specify the SMTP configuration, it will default to the CDC SMTP gateway email server configuration. 

### Logger Notification Setup
No additional settings are needed when using the logger dispatcher.

### Webhook Notification Setup
No additional settings are needed when using the webhook dispatcher.

## Use in ktor applications with Koin
For your convenience, a koin creator for the email dispatcher is available.  To utilize it:

```kotlin
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val notificationDispatcherModule = NotificationDispatcherKoinCreator.moduleFromAppEnv(environment)
    return modules(listOf(notificationDispatcherModule))
}
```
The above call to code will create a singleton of the email dispatcher, which can then be easily used anywhere in your code as follows.
```kotlin
class SomeClass : KoinComponent {
    private val notifications by inject<NotificationDispatcher>()
    
    fun notifyViaEmail() {
        notifications.send(
            EmailNotificationContent(
                "to@email.com",
                "from@email.com",
                "Some System",
                "My First Notification",
                "Hello world"
            )
        )
    }
    
    fun notifyViaWebhook() {
        notifications.send(
            WebhookNotificationContent(
                "https://webhook.site/{{uuid}}",
                "Hello world"
            )
        )
    }
}
```