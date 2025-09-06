package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/test")
public class TestRabbitController {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public TestRabbitController(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/send-test")
    public String sendTestMessage() {
        try {
            Aircraft testAircraft = new Aircraft();
            testAircraft.setId(999L);
            testAircraft.setCallsign("TEST123");
            testAircraft.setReg("TESTREG");
            testAircraft.setAltitude(35000);
            testAircraft.setLat(51.5074);
            testAircraft.setLon(-0.1278);
            testAircraft.setLastSeenTime(Instant.now());

            // Конвертируем объект в JSON строку
            String jsonMessage = objectMapper.writeValueAsString(testAircraft);
            rabbitTemplate.convertAndSend("aircraftpositions", jsonMessage);

            return "Test message sent: " + testAircraft.getCallsign();
        } catch (Exception e) {
            return "Error sending message: " + e.getMessage();
        }
    }

    @GetMapping("/check")
    public String check() {
        return "Test controller is working!";
    }
}