package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/orders")
class OrderController {

    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserOrders(@PathVariable String userId) {
        // 시뮬레이션: 데이터베이스 조회 지연
        try {
            Thread.sleep(100); // 100ms 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("orders", List.of(
            Map.of("orderId", "order-" + userId + "-1", "amount", 100.0, "status", "completed"),
            Map.of("orderId", "order-" + userId + "-2", "amount", 250.0, "status", "pending"),
            Map.of("orderId", "order-" + userId + "-3", "amount", 75.0, "status", "shipped")
        ));
        response.put("totalOrders", 3);
        response.put("service", "order-service");
        response.put("version", "3.0");
        response.put("lastUpdated", java.time.LocalDateTime.now().toString());
        
        return response;
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        try {
            Thread.sleep(50); // 50ms 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("amount", 150.0);
        order.put("status", "completed");
        order.put("items", List.of("item1", "item2"));
        order.put("service", "order-service");
        
        return order;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "order-service", "version", "3.0", "message", "Improved Pipeline Order Service");
    }
}
