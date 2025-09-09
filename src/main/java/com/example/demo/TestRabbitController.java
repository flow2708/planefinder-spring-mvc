package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Random;

@RestController
@RequestMapping("/test")
public class TestRabbitController {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public TestRabbitController(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/send-test")
    public Mono<String> sendTestMessage() {
        return Mono.fromCallable(() -> {
            Aircraft testAircraft = createTestAircraft();
            String jsonMessage = objectMapper.writeValueAsString(testAircraft);
            rabbitTemplate.convertAndSend("aircraftpositions", jsonMessage);
            return "Test message sent: " + testAircraft.getCallsign();
        }).onErrorResume(e -> Mono.just("Error sending message: " + e.getMessage()));
    }

    @GetMapping("/send-multiple")
    public Mono<String> sendMultipleTestMessages() {
        return Mono.fromRunnable(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Aircraft testAircraft = createTestAircraft();
                    testAircraft.setId((long) (1000 + i));
                    testAircraft.setCallsign("TEST" + (100 + i));
                    String jsonMessage = objectMapper.writeValueAsString(testAircraft);
                    rabbitTemplate.convertAndSend("aircraftpositions", jsonMessage);
                    System.out.println("Sent: " + testAircraft.getCallsign());
                } catch (Exception e) {
                    System.err.println("Error sending message " + i + ": " + e.getMessage());
                }
            }
        }).then(Mono.just("5 test messages sent"));
    }

    private Aircraft createTestAircraft() {
        Aircraft testAircraft = new Aircraft();
        testAircraft.setId(random.nextLong());
        testAircraft.setCallsign("TEST" + random.nextInt(1000));
        testAircraft.setReg("TESTREG");
        testAircraft.setAltitude(30000 + random.nextInt(10000));
        testAircraft.setSpeed(400 + random.nextInt(300));
        testAircraft.setLat(51.5074 + (random.nextDouble() - 0.5) * 2);
        testAircraft.setLon(-0.1278 + (random.nextDouble() - 0.5) * 2);
        testAircraft.setLastSeenTime(Instant.now());
        return testAircraft;
    }

    @GetMapping("/check")
    public Mono<String> check() {
        return Mono.just("Test controller is working!");
    }
}