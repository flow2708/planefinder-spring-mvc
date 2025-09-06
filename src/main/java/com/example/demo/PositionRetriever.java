package com.example.demo;

import com.example.demo.WebSocket.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@AllArgsConstructor
@Configuration
public class PositionRetriever {
    private final AircraftRepository repository;
    private final WebSocketHandler handler;
    private final ObjectMapper objectMapper;

    @Bean
    Consumer<Flux<String>> retrieveAircraftPositions() {
        return jsonMessages -> {
            System.out.println("Consumer registered, waiting for JSON messages...");

            jsonMessages
                    .doOnNext(json -> System.out.println("RECEIVED JSON: " + json))
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, Aircraft.class);
                        } catch (Exception e) {
                            System.err.println("Error parsing JSON: " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(aircraft -> aircraft != null)
                    .doOnNext(ac -> System.out.println("PARSED AIRCRAFT: " + ac.getCallsign() + " - " + ac.getReg()))
                    .collectList()
                    .flatMap(aircrafts -> {
                        System.out.println("Processing " + aircrafts.size() + " aircraft");
                        if (aircrafts.isEmpty()) {
                            System.out.println("No aircraft received, skipping database operations");
                            return Mono.empty();
                        }
                        return repository.deleteAll()
                                .doOnSuccess(v -> System.out.println("Database cleared"))
                                .thenMany(repository.saveAll(aircrafts))
                                .doOnNext(saved -> System.out.println("Saved: " + saved.getCallsign()))
                                .then();
                    })
                    .then(Mono.fromRunnable(() -> {
                        System.out.println("Calling sendPositionsReactive");
                        sendPositionsReactive();
                    }))
                    .doOnError(e -> System.err.println("FINAL ERROR: " + e.getMessage()))
                    .subscribe(
                            v -> System.out.println("Processing completed successfully"),
                            e -> System.err.println("Subscription error: " + e.getMessage()),
                            () -> System.out.println("Subscription completed")
                    );
        };
    }

    private void sendPositionsReactive() {
        System.out.println("Sending positions via WebSocket");
        repository.findAll()
                .collectList()
                .doOnNext(aircraftList -> System.out.println("Found " + aircraftList.size() + " aircraft in DB"))
                .subscribe(aircraftList -> {
                    if (!aircraftList.isEmpty()) {
                        String message = aircraftList.toString();
                        List<WebSocketSession> sessions = handler.getSessionList();
                        System.out.println("Active WebSocket sessions: " + sessions.size());

                        for (WebSocketSession session : sessions) {
                            try {
                                if (session.isOpen()) {
                                    session.sendMessage(new TextMessage(message));
                                    System.out.println("Sent WebSocket message to session: " + session.getId());
                                }
                            } catch (IOException e) {
                                System.err.println("Error sending WebSocket message: " + e.getMessage());
                            }
                        }
                    } else {
                        System.out.println("No aircraft in database to send via WebSocket");
                    }
                });
    }
}