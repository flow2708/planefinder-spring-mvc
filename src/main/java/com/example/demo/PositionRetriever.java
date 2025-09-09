package com.example.demo;

import com.example.demo.WebSocket.ReactiveWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor
@Component
public class PositionRetriever {
    private final AircraftRepository repository;
    private final ReactiveWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    @Bean
    public Consumer<Flux<String>> retrieveAircraftPositions() {
        return jsonMessages -> {
            log.info("Consumer function registered and ready to receive messages");

            jsonMessages
                    .doOnNext(json -> log.info("RECEIVED JSON: {}", json))
                    .flatMap(json -> parseAircraft(json)
                            .onErrorResume(e -> {
                                log.error("Error parsing JSON: {}", e.getMessage());
                                return Mono.empty();
                            }))
                    .buffer(10)
                    .flatMap(aircrafts -> processAircraftBatch(aircrafts))
                    .then(Mono.defer(this::sendPositionsReactive))
                    .subscribe(
                            v -> log.info("Processing completed successfully"),
                            e -> log.error("Subscription error: {}", e.getMessage()),
                            () -> log.info("Subscription completed")
                    );
        };
    }

    private Mono<Aircraft> parseAircraft(String json) {
        return Mono.fromCallable(() -> {
                    Aircraft aircraft = objectMapper.readValue(json, Aircraft.class);
                    log.info("PARSED AIRCRAFT: {} - {}", aircraft.getCallsign(), aircraft.getReg());
                    return aircraft;
                })
                .onErrorResume(e -> {
                    log.error("Failed to parse aircraft from JSON: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> processAircraftBatch(java.util.List<Aircraft> aircrafts) {
        if (aircrafts.isEmpty()) {
            log.info("No aircraft received, skipping database operations");
            return Mono.empty();
        }

        log.info("Processing {} aircraft", aircrafts.size());

        return repository.deleteAll()
                .doOnSuccess(v -> log.info("Database cleared"))
                .onErrorResume(e -> {
                    log.error("Failed to clear database: {}", e.getMessage());
                    return Mono.empty();
                })
                .thenMany(repository.saveAll(aircrafts))
                .doOnNext(saved -> log.info("Saved: {}", saved.getCallsign()))
                .doOnError(e -> log.error("Failed to save aircraft: {}", e.getMessage()))
                .then();
    }

    private Mono<Void> sendPositionsReactive() {
        log.info("Sending positions via WebSocket");

        return repository.findAll()
                .collectList()
                .doOnNext(aircraftList -> log.info("Found {} aircraft in DB", aircraftList.size()))
                .onErrorResume(e -> {
                    log.error("Failed to retrieve aircraft from database: {}", e.getMessage());
                    return Mono.just(java.util.Collections.emptyList());
                })
                .flatMap(aircraftList -> {
                    if (aircraftList.isEmpty()) {
                        log.info("No aircraft in database to send via WebSocket");
                        return Mono.empty();
                    }

                    String message = aircraftList.toString();
                    return handler.sendToAll(message)
                            .doOnSuccess(v -> log.info("Sent WebSocket message to all active sessions"))
                            .doOnError(e -> log.error("Failed to send WebSocket message: {}", e.getMessage()));
                });
    }
}