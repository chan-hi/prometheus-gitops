package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RestController
@RequestMapping("/api/users")
class UserController {

    private final RestTemplate restTemplate;

    public UserController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/{userId}")
    public Map<String, Object> getUser(@PathVariable String userId) {
        // Order Service 호출
        String orderServiceUrl = "http://order-service:8080/api/orders/user/" + userId;
        Map<String, Object> orders;
        
        try {
            orders = restTemplate.getForObject(orderServiceUrl, Map.class);
        } catch (Exception e) {
            orders = Map.of("error", "Order service unavailable");
        }

        // Payment Service 호출
        String paymentServiceUrl = "http://payment-service:8080/api/payments/user/" + userId;
        Map<String, Object> payments;
        
        try {
            payments = restTemplate.getForObject(paymentServiceUrl, Map.class);
        } catch (Exception e) {
            payments = Map.of("error", "Payment service unavailable");
        }

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("name", "User " + userId + " (Improved CI/CD v3.0)");
        user.put("email", "user" + userId + "@example.com");
        user.put("version", "3.0");
        user.put("lastUpdated", java.time.LocalDateTime.now().toString());
        user.put("orders", orders);
        user.put("payments", payments);
        user.put("service", "user-service");
        
        return user;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "user-service", "version", "3.0", "message", "Improved CI/CD Pipeline");
    }
}
