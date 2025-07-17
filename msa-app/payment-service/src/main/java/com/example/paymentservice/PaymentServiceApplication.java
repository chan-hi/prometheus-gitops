package com.example.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/payments")
class PaymentController {

    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserPayments(@PathVariable String userId) {
        // 시뮬레이션: 외부 결제 시스템 호출 지연
        try {
            Thread.sleep(200); // 200ms 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("payments", List.of(
            Map.of("paymentId", "pay-" + userId + "-1", "amount", 100.0, "status", "completed", "method", "credit_card"),
            Map.of("paymentId", "pay-" + userId + "-2", "amount", 250.0, "status", "pending", "method", "bank_transfer"),
            Map.of("paymentId", "pay-" + userId + "-3", "amount", 75.0, "status", "completed", "method", "paypal")
        ));
        response.put("totalAmount", 425.0);
        response.put("service", "payment-service");
        response.put("version", "3.0");
        response.put("lastUpdated", java.time.LocalDateTime.now().toString());
        
        return response;
    }

    @GetMapping("/{paymentId}")
    public Map<String, Object> getPayment(@PathVariable String paymentId) {
        try {
            Thread.sleep(80); // 80ms 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("amount", 200.0);
        payment.put("status", "completed");
        payment.put("method", "credit_card");
        payment.put("service", "payment-service");
        
        return payment;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "payment-service", "version", "3.0", "message", "Enhanced Payment Gateway v3.0");
    }
}
