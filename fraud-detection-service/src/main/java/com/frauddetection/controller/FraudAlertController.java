package com.frauddetection.controller;

import com.frauddetection.model.FraudAlert;
import com.frauddetection.service.FraudAlertRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Backs a simple dashboard (React/Grafana/whatever you build) showing
 * live-ish fraud alerts. Poll this every few seconds for a v1 "live" feel;
 * swap for a WebSocket/SSE push later if you want true real-time.
 */
@RestController
public class FraudAlertController {

    private final FraudAlertRepository repository;

    public FraudAlertController(FraudAlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/v1/alerts")
    public List<FraudAlert> recentAlerts(@RequestParam(defaultValue = "50") int limit) {
        return repository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "flaggedAt")))
                .getContent();
    }
}
