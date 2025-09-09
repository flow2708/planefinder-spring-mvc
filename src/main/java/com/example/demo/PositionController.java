package com.example.demo;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Controller
public class PositionController {
    @NonNull
    private final AircraftRepository repository;

    @GetMapping("/aircraft")
    public Mono<String> getCurrentAircraftPositions(Model model) {
        System.out.println("Request to /aircraft received");

        return repository.findAll()
                .collectList()
                .doOnNext(aircraftList -> {
                    System.out.println("Found " + aircraftList.size() + " aircraft in database");
                    model.addAttribute("currentPositions", aircraftList);
                })
                .thenReturn("positions")
                .doOnSuccess(template -> System.out.println("Returning template: " + template))
                .doOnError(e -> System.err.println("Error in /aircraft: " + e.getMessage()));
    }
}