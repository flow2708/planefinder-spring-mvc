package com.example.demo;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Controller
public class PositionController {
    /*private WebClient client =
            WebClient.create("http://localhost:7634/api/aircraft");
            больше не нужен по причине перехода на платформу обмена сообщений RabbitMQ
            */
    @NonNull
    private final AircraftRepository repository;

    @GetMapping("/aircraft")
    public String getCurrentAircraftPositions(Model model) {
        model.addAttribute("currentPositions", repository.findAll());
        return "positions";
    }
}
