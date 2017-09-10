package eu.dragulescu.http2rmq

import org.glassfish.grizzly.http.Method
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.grizzly.http.util.HttpStatus

object HttpModule {
    private var server : HttpServer? = null
    fun connect(serverPath:String, endpointPath:String, serverPort:Int) : HttpServer {
        if (server != null) return server!!

        server = HttpServer.createSimpleServer(serverPath,serverPort)

        server!!.serverConfiguration
            .addHttpHandler(
                object : HttpHandler(){
                    override fun service(request: Request?, response: Response?) {
                        if (request == null || response == null) return

                        val directions = extractDirections(request.httpHandlerPath)
                        if (directions == null) {response.setStatus(HttpStatus.BAD_REQUEST_400);return}

                        val headers = extractHeaders(request)

                        when (request.method) {
                            Method.POST -> {
                                RabbitMqModule.post(
                                        directions.first,
                                        directions.second,
                                        request.getPostBody(request.contentLength).toStringContent(),
                                        headers
                                        )
                                response.setStatus(HttpStatus.OK_200)
                            }
                            Method.GET -> {
                                response.writer.write(
                                        RabbitMqModule.get(
                                                directions.first,
                                                directions.second,
                                                headers
                                        )
                                )
                                response.setStatus(HttpStatus.OK_200)
                                return
                            }
                            else -> {response.setStatus(HttpStatus.FORBIDDEN_403);return;}
                        }
                    }
                }, endpointPath)
        return server!!
    }

    fun start() {
        if (server?.isStarted == true) return

        server?.start()
    }

    fun disconnect() {
        server?.shutdownNow()
    }

    private fun extractHeaders(request: Request): Map<String, Any> {
        val headers = HashMap<String,Any>()

        request.headerNames.forEach { headers.putIfAbsent(it, request.getHeader(it)) }

        return headers
    }

    private fun extractDirections(from:String) : Pair<String,String>? {
        val targets = from.trimStart('/').split('/')
        if (targets.size != 2) return null
        return Pair(targets.first(), targets.last())
    }
}
