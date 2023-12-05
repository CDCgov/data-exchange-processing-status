package gov.cdc.ocio.processingstatusapi.model


open class HealthCheckSystem() {
    var status: String = "DOWN"
    var health_issues: String? = ""

    fun health_issues(message: String?) {
        health_issues = message
    }
}

class CosmosDb: HealthCheckSystem(){
    var service: String = "Cosmos DB"
}

class ServiceBus: HealthCheckSystem(){
    var service: String = "Azure Service Bus"
}

class HealthCheck {
    fun total_checks_duration(time: String) {
        this.total_checks_duration = time;
    }

    var status : String? = "DOWN"

    var total_checks_duration : String?=null

    var dependency_health_checks : ArrayList<HealthCheckSystem> = arrayListOf()

}
