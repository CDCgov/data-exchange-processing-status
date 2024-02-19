package gov.cdc.ocio.rulesEngine

class WebsocketNotificationRule(): Rule {
    override fun evaluate(ruleId: String): Boolean {
        println("Websocket Rule Matched")
        return true;
    }
}