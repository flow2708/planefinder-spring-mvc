package com.example.demo;

import com.example.demo.WebSocket.ReactiveWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@AllArgsConstructor
@Component
public class PositionRetriever {
    private final AircraftRepository repository;
    private final ReactiveWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    @Bean
    public Consumer<Flux<String>> retrieveAircraftPositions() {
        return jsonMessages -> {
            System.out.println("Consumer registered, waiting for JSON messages...");

            jsonMessages
                    .doOnNext(json -> System.out.println("RECEIVED JSON: " + json))
                    .flatMap(json -> parseAircraft(json)
                            .onErrorResume(e -> {
                                System.err.println("Error parsing JSON: " + e.getMessage());
                                return Mono.empty();
                            }))
                    .buffer(100)
                    .flatMap(aircrafts -> processAircraftBatch(aircrafts))
                    .then(Mono.defer(this::sendPositionsReactive))
                    .subscribe(
                            v -> System.out.println("Processing completed successfully"),
                            e -> System.err.println("Subscription error: " + e.getMessage()),
                            () -> System.out.println("Subscription completed")
                    );
        };
    }

    private Mono<Aircraft> parseAircraft(String json) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, Aircraft.class))
                .doOnNext(ac -> System.out.println("PARSED AIRCRAFT: " + ac.getCallsign() + " - " + ac.getReg()))
                .onErrorResume(e -> {
                    System.err.println("Failed to parse aircraft from JSON: " + e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> processAircraftBatch(java.util.List<Aircraft> aircrafts) {
        if (aircrafts.isEmpty()) {
            System.out.println("No aircraft received, skipping database operations");
            return Mono.empty();
        }

        System.out.println("Processing " + aircrafts.size() + " aircraft");

        return repository.deleteAll()
                .doOnSuccess(v -> System.out.println("Database cleared"))
                .onErrorResume(e -> {
                    System.err.println("Failed to clear database: " + e.getMessage());
                    return Mono.empty();
                })
                .thenMany(repository.saveAll(aircrafts))
                .doOnNext(saved -> System.out.println("Saved: " + saved.getCallsign()))
                .doOnError(e -> System.err.println("Failed to save aircraft: " + e.getMessage()))
                .then();
    }

    private Mono<Void> sendPositionsReactive() {
        System.out.println("Sending positions via WebSocket");

        return repository.findAll()
                .collectList()
                .doOnNext(aircraftList -> System.out.println("Found " + aircraftList.size() + " aircraft in DB"))
                .onErrorResume(e -> {
                    System.err.println("Failed to retrieve aircraft from database: " + e.getMessage());
                    return Mono.just(java.util.Collections.emptyList());
                })
                .flatMap(aircraftList -> {
                    if (aircraftList.isEmpty()) {
                        System.out.println("No aircraft in database to send via WebSocket");
                        return Mono.empty();
                    }

                    String message = aircraftList.toString();
                    return handler.sendToAll(message)
                            .doOnSuccess(v -> System.out.println("Sent WebSocket message to all active sessions"))
                            .doOnError(e -> System.err.println("Failed to send WebSocket message: " + e.getMessage()));
                });
    }
}