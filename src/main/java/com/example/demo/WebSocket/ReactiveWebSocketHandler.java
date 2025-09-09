package com.example.demo.WebSocket;

import com.example.demo.AircraftRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class ReactiveWebSocketHandler implements WebSocketHandler {

    @NonNull
    private final AircraftRepository repository;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        System.out.println("Connection established from " + session.getId() + " @ " + Instant.now());
        sessions.put(session.getId(), session);

        // Обработка входящих сообщений
        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message ->
                        System.out.println("Message received: '" + message + "', from " + session.getId()))
                .flatMap(message -> broadcastMessage(message, session.getId()))
                .onErrorResume(e -> {
                    System.out.println("Exception handling message: " + e.getLocalizedMessage());
                    return Mono.empty();
                })
                .then();

        // Обработка закрытия соединения
        Mono<Void> completion = session.closeStatus()
                .doOnNext(closeStatus -> {
                    sessions.remove(session.getId());
                    System.out.println("Connection closed by " + session.getId() + " @ " + Instant.now());
                })
                .then();

        return Mono.when(input, completion);
    }

    private Mono<Void> broadcastMessage(String message, String senderSessionId) {
        return Flux.fromIterable(sessions.values())
                .filter(session -> !session.getId().equals(senderSessionId) && session.isOpen())
                .flatMap(session -> session.send(Mono.just(session.textMessage(message)))
                        .doOnSuccess(v ->
                                System.out.println("--> Sending message '" + message + "' to " + session.getId()))
                        .onErrorResume(e -> {
                            System.out.println("Error sending to session " + session.getId() + ": " + e.getMessage());
                            return Mono.empty();
                        }))
                .then();
    }

    public Flux<WebSocketSession> getActiveSessions() {
        return Flux.fromIterable(sessions.values())
                .filter(WebSocketSession::isOpen);
    }

    public Mono<Void> sendToAll(String message) {
        return getActiveSessions()
                .flatMap(session -> session.send(Mono.just(session.textMessage(message))))
                .then();
    }
}