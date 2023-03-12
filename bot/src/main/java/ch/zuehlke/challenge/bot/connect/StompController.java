package ch.zuehlke.challenge.bot.connect;

import ch.zuehlke.challenge.bot.util.ApplicationProperties;
import ch.zuehlke.common.GameId;
import ch.zuehlke.common.GameUpdate;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;

@Slf4j
@Controller
@RequiredArgsConstructor
// Adapted from tutorial at https://blog.dkwr.de/development/spring/spring-stomp-client/
public class StompController implements StompSessionHandler {

    private final ApplicationProperties applicationProperties;

    private StompSession stompSession;

    private StompSession.Subscription subscription;

    @EventListener(value = ApplicationReadyEvent.class)
    public void connect() {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        try {
            String socketUrl = applicationProperties.getWebSocketUri();
            stompSession = stompClient
                    .connect(socketUrl, this) // Improve: Don't use deprecated method
                    .get();
        } catch (Exception e) {
            log.error("Connection failed.", e); // Improve: error handling.
        }
    }

    private void subscribe(Integer gameId) {
        log.info("Subscribing to id: {}", gameId);
        this.subscription = stompSession.subscribe("/topic/game/" + gameId, this);

        // test out sending a message, TODO: remove in the future
        stompSession.send("/topic/game/" + gameId, new GameUpdate(new GameId(gameId)));
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Connection to STOMP server established");
        subscribe(applicationProperties.getGameId());
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        log.error("Got an exception while handling a frame.\n" +
                "Command: {}\n" +
                "Headers: {}\n" +
                "Payload: {}\n" +
                "{}", command, headers, payload, exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("Retrieved a transport error: {}", session);
        exception.printStackTrace();
        if (!session.isConnected()) {
            this.subscription = null;
            connect();
        }
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return GameUpdate.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        log.info("Got a new message {}", payload);
        GameUpdate stompMessage = (GameUpdate) payload;
        // TODO: Consume the message, handle possible ClassCastExceptions or different payload classes.
    }

    @PreDestroy
    void onShutDown() {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
        }

        if (stompSession != null) {
            stompSession.disconnect();
        }
    }
}