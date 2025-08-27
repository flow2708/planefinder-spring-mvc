package com.example.demo;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Consumer;

@AllArgsConstructor
@Configuration
public class PositionRetriever {
    private final AircraftRepository aircraftRepository;

    @Bean
    Consumer<List<Aircraft>> retrieveAircraftPositions() {
        return acList -> {
            aircraftRepository.deleteAll();

            aircraftRepository.saveAll(acList);

            aircraftRepository.findAll().forEach(System.out::println);
        };
    }
}
