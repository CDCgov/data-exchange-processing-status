package gov.cdc.ocio.model

import com.google.gson.annotations.SerializedName


open class HealthCheckSystem {

    var status: String = "DOWN"

    @SerializedName("health_issues")
    var healthIssues: String? = ""
}

class ServiceBus: HealthCheckSystem() {
    var service: String = "Azure Service Bus"
}

class HealthCheck {

    var status : String? = "DOWN"

    @SerializedName("total_checks_duration")
    var totalChecksDuration : String? = null

    @SerializedName("dependency_health_checks")
    var dependencyHealthChecks = arrayListOf<HealthCheckSystem>()
}
