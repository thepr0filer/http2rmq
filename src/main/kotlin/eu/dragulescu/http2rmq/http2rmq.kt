package eu.dragulescu.http2rmq

import java.util.concurrent.atomic.AtomicBoolean

fun main(args:Array<String>) {
    if (args.size != 4) throw IllegalArgumentException(
            "4 args expected: <serverpath> <endpointpath> <serverport> <amqpconnectionstring>: i.e.: \"\" 9099 / amqp://app_user:app_pass@rmq_broker:rmq_port/vhost")

    val sf = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread({sf.set(true)}))

    RabbitMqModule.connect(args[3])
    HttpModule.connect(args[0], args[2], args[1].toInt())

    HttpModule.start()

    while (!sf.get()) {
        Thread.sleep(20)
    }

    HttpModule.disconnect()
    RabbitMqModule.disconnect()
}
