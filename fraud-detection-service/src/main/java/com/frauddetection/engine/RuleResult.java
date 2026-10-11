package com.frauddetection.engine;

import java.util.ArrayList;
import java.util.List;

public class RuleResult {

    private final List<String> triggeredReasons = new ArrayList<>();

    public void flag(String reason) {
        triggeredReasons.add(reason);
    }

    public boolean isSuspicious() {
        return !triggeredReasons.isEmpty();
    }

    public List<String> getTriggeredReasons() {
        return triggeredReasons;
    }
}
