package com.paymentservice.controller;

import com.paymentservice.model.Transaction;
import com.paymentservice.model.TransactionRequest;
import com.paymentservice.repository.TransactionRepository;
import com.paymentservice.service.TransactionProducer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class PaymentController {

    private final TransactionRepository repository;
    private final TransactionProducer producer;

    public PaymentController(TransactionRepository repository, TransactionProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    /**
     * Accepts a transaction, persists it as PENDING, and publishes it to
     * Kafka for async fraud evaluation. Deliberately does NOT wait for the
     * fraud verdict here — that's the whole point of the async design.
     * A real system would either (a) return PENDING and let the client poll
     * /status, or (b) hold a short synchronous window (e.g. 200ms) for the
     * rule-engine fast-path only. This skeleton does (a) for simplicity.
     */
    @PostMapping
    public ResponseEntity<Transaction> submitTransaction(@Valid @RequestBody TransactionRequest request) {
        Transaction txn = Transaction.newTransaction(
                request.getUserId(),
                request.getAmount(),
                request.getMerchant(),
                request.getDeviceId(),
                request.getLocation()
        );

        repository.save(txn);
        producer.publish(txn);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(txn);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getStatus(@PathVariable String transactionId) {
        return repository.findById(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
