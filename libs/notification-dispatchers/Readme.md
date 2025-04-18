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

### Email Dispatcher Usage
In the `application.conf` you will need to specify the email dispatcher type. Use environment variable
`EMAIL_DISPATCHER` to indicate which type to use. The supported types are:
- `SMTP` - Uses Standard Mail Transfer Protocol to send emails.
- `LOGGER` - Will only log that an email dispatch was attempted, but nothing is actually sent.
When adding support for the notifications dispatcher to your ktor application, you will need to specify the email dispatcher in the `ktor` section of `application.conf`.
```
ktor {
    emailDispatcher = "SMTP" # default to SMTP if EMAIL_DISPATCHER missing
    emailDispatcher = ${?EMAIL_DISPATCHER}
}
```

#### SMTP Email Dispatcher Setup
```
smtp {
    host = sandbox.smtp.mailtrap.io
    port = 25
    auth = true
    username = someuser
    password = somepassword
}
```
- `host` - hostname of the SMTP server
- `port` - port number to use, typically 25
- `auth` - if true, then the `username` and `password` is used to authenticate with the SMTP server
- `username` - username used for SMTP server auth
- `password` - password used for SMTP server auth
> NOTE: If you do not specify the SMTP configuration, it will default to the CDC SMTP gateway email server configuration. 

#### Logger Email Dispatcher Setup
No additional settings are needed when using the logger email dispatcher.  Simply set the `EMAIL_DISPATCHER` to `LOGGER` and that's it.