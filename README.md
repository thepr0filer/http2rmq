# http2rmq
A basic server application that accepts get and post http requests and redirects them to a rabbitmq broker

# building
Gradle project build / publish

# installing
Gradle installZip

# running
unzip and run http2rmq[.bat] with parameters: httpServerPath httpEndpointPath httpServerPort httpServerPort amqpConnectionURI
example: http2rmq "" 9099 / amqp://app_user:app_pass@rmq_broker:rmq_port/vhost
    - will start locally an http server on port 9099 handling GET and POST requests
    - all requests need to be issued with /exchange/topic path
    - GET localhost:9099/exchange/topic will generate a request to the rmq exchange on the topic and wait for the response
    - POST localhost:9099/exchange/topic will post the content to the exchange with the topic
