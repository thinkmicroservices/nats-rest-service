The service requires a local NATS instance running on port 4222.
The following docker command will spin up a working instance the
service can use

docker run -p 4222:4222 -p 8222:8222 -p 6222:6222   nats:latest


To build the application as a jar with included dependencies:

mvn clean package

To run a single instance of the jar application:

java -jar ./target/nats-processor-service-0.0.1-SNAPSHOT.jar 

To run multiple instance of the jar we need to override a couple application 
properties. The following three commands will start three queu workers on 
different ports, with different names (imaginatively names QW-1, QW-2, and QW-3).

java -jar ./target/nats-queue-worker-service-0.0.1-SNAPSHOT.jar --queue.worker.name=QW-1 --server.port=8100
java -jar ./target/nats-queue-worker-service-0.0.1-SNAPSHOT.jar --queue.worker.name=QW-2 --server.port=8110
java -jar ./target/nats-queue-worker-service-0.0.1-SNAPSHOT.jar --queue.worker.name=QW-3 --server.port=8120


To build the Spring Native GraalVM native executable

mvn clean spring-boot:build-image


Once all services have started (NATSRestService, NATSProcessorService, and NATSQueueWorkerService), we
can call the Fire and Forget method with the following command:

curl http://localhost:8080/fnf/{message}

where {message} is one of the following:

 cap - capitalizes the message.
 lc  - converts all characters to lower-case.
 rev - reverses all characters.
 uc  - converts all characters to upper-case.

Once the FnF operation has been selected (or the default is used) we can call
the Request-Reply method with the following command:

curl http://localhost:8080/request-reply/{message}

where {message} is the string we want to process.

responses will be in the form:

<{incoming-message}> processed by <{queue-worker-name}> transformed into ({processed-message})


If you are running multiple queue workers and only seeing a single queue worker
performing all operations, it may be that enough load isn't be put on the
set of services. We can add additional load by calling curl via xargs. Here
we make 10 parallel calls to the curl endpoint 400 time. 

xargs -I % -P 10 curl "http://localhost:8080/request-reply/a-message" \
< <(printf '%s\n\n' {1..400})
