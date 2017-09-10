import com.rabbitmq.client.AMQP
import eu.dragulescu.http2rmq.RabbitMqModule
import org.junit.Test

class TestResponder {
    @Test
    fun runResponderForEventsRetrieve() {
        class QH : RabbitMqModule.NamedConsumer() {
            override fun handle(request: String, properties: AMQP.BasicProperties) {
                if (properties.replyTo.isNullOrBlank()) return

                val props = AMQP.BasicProperties.Builder()
                        .replyTo(properties.replyTo)
                        .correlationId(properties.correlationId)
                        .build()
                RabbitMqModule.post("", properties.replyTo, "bounced back:$request", props)
            }
        }

        RabbitMqModule.connect("amqp://app_user:app_pass@localhost:5672/eonly")

        val handler = QH()
        RabbitMqModule.registerHandler("", mapOf(Pair("events","retrieve")), handler)

        while (RabbitMqModule.isConnected() == true) {
            Thread.sleep(20)
        }

        RabbitMqModule.deregisterHandler(handler)
    }
}