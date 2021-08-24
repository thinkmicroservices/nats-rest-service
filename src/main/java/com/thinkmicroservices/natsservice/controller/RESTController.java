package com.thinkmicroservices.natsservice.controller;

import io.nats.client.Connection;
import io.nats.client.Connection.Status;
import io.nats.client.ConnectionListener;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import java.io.IOException;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class provide a REST endpoints to invoke a <i>Fire-and-Forget</i>
 * message to change the <i>QueueWorker's</i> message operation, amd a 
 * <i>request/reply</i> message to invoke the <i>QueueWorker's</i>.
 * @author cwoodward
 */
@RestController
@Slf4j
public class RESTController implements ConnectionListener {

    @Value("${nats.servers}")
    private String[] servers;

    private Connection connection;

    private static final String FNF_SUBJECT = "nats.fnf";
    private static final String REQUEST_REPLY_SUBJECT = "nats.request-reply";

    @GetMapping(value = "/fnf/{message}")
    public void fireAndForget(@PathVariable String message) throws IOException, InterruptedException {

        log.info("Publishing FNF message:" + message);
        getConnection().publish(FNF_SUBJECT, message.getBytes());
    }

    /**
     * Expose the <i>Request/Reply</i> message.
     * @param message
     * @return
     * @throws IOException
     * @throws InterruptedException 
     */
    @GetMapping(value = "/request-reply/{message}")

    public String requestReply(@PathVariable String message) throws IOException, InterruptedException {

        log.info("Publishing Request-Reply message:" + message);

         return getConnection().request(REQUEST_REPLY_SUBJECT, message.getBytes())
                .thenApply(Message::getData)
                 .thenApply(String::new)
                 .join()                ;
                
    }

    /**
     * Create a connection to the configured NATS servers.
     * @return
     * @throws IOException
     * @throws InterruptedException 
     */
    private Connection getConnection() throws IOException, InterruptedException {
        if ((connection == null) || (connection.getStatus() == Status.DISCONNECTED)) {
           
            Options.Builder connectionBuilder = new Options.Builder().connectionListener(this);

            for (String server : servers) {
                String natsServer = "nats://" + server;
                log.info("adding nats server:" + natsServer);
                connectionBuilder.server(natsServer).maxReconnects(-1);
            }

            connection = Nats.connect(connectionBuilder.build());
        }
        log.info("return connection:" + connection);
        return connection;
    }

    @PostConstruct
    void postConstruct() throws IOException, InterruptedException {
        log.info("REST controller postConstruct.");
    }

    /**
     * Listen for NATS connection events.
     * @param cnctn
     * @param event 
     */
    @Override
    public void connectionEvent(Connection cnctn, Events event) {
        log.info("Connection Event:" + event);

        switch (event) {

            case CONNECTED:
                log.info("CONNECTED!");
                break;
            case DISCONNECTED:
                try {
                    connection = null;
                    getConnection();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);

                }
                break;
            case RECONNECTED:
                log.info("RECONNECTED!");
                break;
            case RESUBSCRIBED:
                log.info("RESUBSCRIBED!");
                break;

        }

    }

}
