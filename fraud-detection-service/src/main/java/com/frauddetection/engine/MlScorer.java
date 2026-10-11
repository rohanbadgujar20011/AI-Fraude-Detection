package com.frauddetection.engine;

import com.frauddetection.model.TransactionEvent;

/**
 * Layer 2 fraud check. Deliberately an interface: start with
 * StubMlScorer (heuristic placeholder), later swap in a real
 * implementation that calls a Python/FastAPI model-serving endpoint
 * (trained offline on a labeled dataset, e.g. Kaggle's credit card
 * fraud dataset) without changing anything in FraudConsumer.
 */
public interface MlScorer {
    /**
     * @return fraud probability in [0.0, 1.0]
     */
    double score(TransactionEvent txn);
}
