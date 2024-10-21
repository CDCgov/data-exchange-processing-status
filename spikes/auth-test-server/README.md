## OAuth Token Verification Middleware

- POC test application to demonstrate Oauth 2.0 Token validation using a KTOR install block and interceptor approach

This middleware provides OAuth 2.0 token verification for incoming requests. It currently supports JWT tokens with the plan to add support for
opaque tokens.

#### Configuration

You need to configure the middleware by setting up the following config variables for your OAuth settings. In this test POC
these are values located in the middleware itself. In a real world application these would most likely be moved to env variables.

```
data class AuthConfig(
    val authEnabled: Boolean, val issuerUrl: String, val introspectionUrl: String, val requiredScopes: String?
)

// Dummy appConfig, replace with actual config loading logic
val authConfig = AuthConfig(
    authEnabled = true,
    issuerUrl = "http://localhost:9080/realms/test-realm-jwt",
    introspectionUrl = "https://your-introspection-url",
    requiredScopes = ""
)
```

#### Usage

Setup middleware package in your application replicating the `Auth.kt` file. Then in your main `Application.kt` file enable as follows:

```
fun Application.module() {
    configureAuth()
}
```

Then wrap any route or sets of routes that you want authenticated:

```
fun Application.configureRouting() {
    routing {
        get("/public") {
            call.respondText("You have reached a public route")
        }
        authenticate("oauth") {
            get("/protected") {
                call.respondText("You have reached a protected route, ${call.principal<UserIdPrincipal>()?.name}")
            }
        }
    }
}
```
