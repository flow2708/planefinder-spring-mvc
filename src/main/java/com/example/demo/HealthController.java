package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final AircraftRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/check")
    public Mono<String> healthCheck() {
        return repository.count()
                .map(count -> "Database connection OK. Aircraft count: " + count)
                .onErrorResume(e -> {
                    AvailabilityChangeEvent.publish(eventPublisher, e, LivenessState.BROKEN);
                    return Mono.just("Database connection FAILED: " + e.getMessage());
                });
    }
}