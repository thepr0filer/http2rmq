package eu.dragulescu.http2rmq

import com.rabbitmq.client.*
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

object RabbitMqModule {
    private var amqpString = ""
    private val connecter = ConnectionFactory()
    private var connection : Connection? = null
    private val channels = ConcurrentHashMap<String, Channel>()
    private val channelsRQ = ConcurrentHashMap<String, String>()

    fun isConnected() = connection?.isOpen

    fun connect(connectionString:String) {
        if (amqpString.isNotEmpty()) return
        amqpString = connectionString

        connecter.setUri(amqpString)
        connection = connecter.newConnection()
    }

    fun disconnect() {
        amqpString = ""
        channels.forEach{ _, channel -> channel.close()}
        channels.clear()
        channelsRQ.clear()
        connection?.close()
        connection = null
    }

    fun get(from:String, topic:String, headers:Map<String,Any>, message: String = "") : String {
        val channel = getOrCreateChannel(from, topic)
        val ck = getChannelKey(from,topic)
        val corrId = UUID.randomUUID().toString()

        val props = AMQP.BasicProperties.Builder()
                .headers(headers)
                .correlationId(corrId)
                .replyTo(channelsRQ[getChannelKey(from,topic)])
                .build()
        channel.basicPublish(from, topic, props, message.toByteArray())

        val response = ArrayBlockingQueue<String>(1)
        channel.basicConsume(channelsRQ[ck], true, object : DefaultConsumer(channel) {
            @Throws(IOException::class)
            override fun handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: ByteArray) {
                if (properties.correlationId == corrId) {
                    response.offer(body.toString(Charset.forName("UTF-8")))
                }
            }
        })
        return response.take()
    }

    fun post(to:String, topic:String, message:String, headers:Map<String,Any>) : String
    {
        val props = AMQP.BasicProperties.Builder()
                .headers(headers)
                .build()
        return post(to, topic, message, props)
    }
    fun post(to:String, topic:String, message:String, props:AMQP.BasicProperties) : String
    {
        val channel = getOrCreateChannel(to, topic)
        channel.basicPublish(to, topic, props, message.toByteArray())
        return ""
    }

    abstract class NamedConsumer(val name:String = UUID.randomUUID().toString()) {
        abstract fun handle(request:String, properties: AMQP.BasicProperties)
    }

    fun registerHandler(consumingQueue:String, sources:Map<String,String>, handler:NamedConsumer) {
        val channel = getOrCreateChannel(handler.name)

        var cq = consumingQueue
        if (consumingQueue.isEmpty()) {
            cq = channel.queueDeclare().queue
        }

        sources.forEach { exchange, topic ->
            channel.queueBind(cq, exchange, topic)
        }

        channel.basicConsume(cq, object : DefaultConsumer(channel) {
            @Throws(IOException::class)
            override fun handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: ByteArray) {
                handler.handle(body.toString(Charset.forName("UTF-8")), properties)
            }
        })
    }

    fun deregisterHandler(handler: NamedConsumer) {
        getChannel(handler.name)?.close()
    }

    private fun getOrCreateChannel(exchange:String, topic:String)  = getOrCreateChannel(getChannelKey(exchange, topic))

    private fun getOrCreateChannel(channelKey:String) : Channel {
        var channel = getChannel(channelKey)
        if (channel == null) channel = setChannel(channelKey, connection!!.createChannel())
        return channel
    }
    private fun getChannelKey(exchange:String, topic:String) = "$exchange?$topic"
    private fun getChannel(exchange:String, topic:String) = getChannel(getChannelKey(exchange, topic))
    private fun getChannel(channelKey:String) = channels.getOrDefault(channelKey, null)
    private fun setChannel(channelKey:String, channel: Channel) : Channel {
        channels.putIfAbsent(channelKey, channel)
        channelsRQ.putIfAbsent(channelKey, channel.queueDeclare().queue)
        return channel
    }
}