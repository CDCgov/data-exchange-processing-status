# notifications-dispatcher
The `notifications-dispatcher` library provides an interface for dispatching notifications for various systems, including emails and webhooks. The `notifications-dispatcher` library is used for processing status report validations. Each report has a schema associated with it that this library can be used to determine whether the report is valid or not and if not, the reasons why. This will contain the interfaces and their implementations, which can be reused across multiple services.

### gradle
Add the following to the `dependencies` of your project' `build.gradle`.
```groovy
dependencies {
    implementation project(':libs:notifications-dispatcher')
}
```
This will allow the `notifications-dispatcher` to be compiled if necessary and linked with your project.  You can set breakpoints in the library the same as you would your main project for debugging.
