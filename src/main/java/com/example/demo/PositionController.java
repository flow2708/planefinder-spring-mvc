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
        return repository.findAll()
                .collectList()
                .doOnNext(aircraftList -> model.addAttribute("currentPositions", aircraftList))
                .thenReturn("positions");
    }
}