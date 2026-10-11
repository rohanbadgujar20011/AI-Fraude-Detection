# Real-Time Event-Driven Fraud Detection Engine

A distributed, low-latency microservice architecture for detecting payment fraud in real time. Built with **Java 17 / Spring Boot 3**, **Apache Kafka**, **Redis**, **PostgreSQL**, and a **Python FastAPI Machine Learning microservice**, this engine evaluates payment transactions through a hybrid two-tier decision pipeline (deterministic rule engine + ML probability scoring).

---

## 🏗️ Architecture

```
Client
  │  POST /api/v1/transactions
  ▼
payment-service (Spring Boot, :8081)
  │  1. Persists transaction as PENDING (Postgres)
  │  2. Publishes event to Kafka topic "transactions" (partitioned by userId)
  ▼
Apache Kafka
  ▼
fraud-detection-service (Spring Boot, :8082)
  │  1) RuleEngine (Layer 1 - Low Latency Redis State):
  │     • Haversine Travel Speed Check (> 800 km/h)
  │     • Exponential Moving Average Amount Spike (> 5x user baseline)
  │     • Sliding Window Velocity Track (> 4 txns/min)
  │     • High-Risk Merchant Category Filter (Crypto, Gambling, Gift Cards)
  │     • New Device + High Value Check
  │
  │  2) RestMlScorer (Layer 2 - REST ML Microservice, :5000):
  │     • Queries FastAPI Random Forest Model for fraud probability
  │     • Automatic fallback to heuristic scoring if REST API offline
  │
  │  3) 3-Tier Risk Decisioning:
  │     • High Risk (≥ 0.75 or Critical Rules)  ──► BLOCKED
  │     • Medium Risk (0.30 - 0.75 / Mild Rules) ──► REVIEW
  │     • Low Risk (< 0.30)                     ──► APPROVED
  ▼
Verdict Settlement:
  │  • Suspicious? Saves FraudAlert (Postgres) + publishes to "fraud-alerts"
  │  • Clean?      Publishes transactionId to "approved"
  ▼
payment-service (VerdictListener)
  │  Updates Postgres transaction status to APPROVED / REVIEW / BLOCKED
  ▼
GET /api/v1/alerts (fraud-detection-service) — Live dashboard & audit feed
```

---

## 🔄 End-to-End Execution Lifecycle & Process Flow

### Step 1: Ingestion & Synchronous Acknowledgment (`payment-service`)
1. **Client POST Request**: Client sends a transaction payload (`userId`, `amount`, `merchant`, `deviceId`, `location`) to `POST /api/v1/transactions`.
2. **Database Persistence**: `payment-service` assigns a unique UUID (`transactionId`), sets initial `status = PENDING` and `timestamp = Instant.now()`, and saves it to the PostgreSQL `transactions` table.
3. **Kafka Event Publishing**: `TransactionProducer` publishes the event to Kafka topic `"transactions"`, explicitly keyed by `userId`. Partitioning by `userId` guarantees that all transactions for a single user are processed sequentially on the same partition.
4. **Immediate Client Response**: Returns HTTP `202 ACCEPTED` with the initial `PENDING` transaction object.

### Step 2: Event Consumption & Layer 1 Rule Engine (`fraud-detection-service`)
1. **Kafka Consumption**: `FraudConsumer` listens on the `"transactions"` topic and receives the `TransactionEvent`.
2. **Deterministic Rule Engine Evaluation (`RuleEngine.java`)**:
   * **Absolute Amount Check**: Compares `amount` against `$100,000`. Flags `AMOUNT_EXCEEDS_ABSOLUTE_LIMIT`.
   * **Amount Spike Check**: Reads `user:{id}:avg_amount` from Redis. Flags `AMOUNT_SPIKE_VS_USER_AVERAGE` if `amount / rollingAvg > 5.0`.
   * **Sliding Window Velocity Check**: Executes atomic Redis `INCR user:{id}:txn_count_1min`. Sets 60-second TTL on key creation. Flags `VELOCITY_LIMIT_EXCEEDED` if `count > 4`.
   * **Geospatial Haversine Travel Speed Check**: Reads `user:{id}:last_location` and `user:{id}:last_location_time` from Redis. Parses coordinates (`lat,lon` or city lookup), calculates distance ($d$ in km) and time elapsed ($\Delta t$ in seconds). If speed $> 800	ext{ km/h}$ over $d > 50	ext{ km}$, flags `IMPOSSIBLE_TRAVEL_SPEED_EXCEEDED`.
   * **High-Risk Merchant Check**: Scans merchant name against keywords (`CRYPTO`, `CASINO`, `GAMBLING`, `GIFT_CARD`, `WIRE_TRANSFER`, `JEWELRY`). Flags `HIGH_RISK_MERCHANT_CATEGORY` for purchases $> \$1,000$.
   * **New Device Check**: Compares `deviceId` with `user:{id}:last_device` in Redis. Flags `NEW_DEVICE_HIGH_VALUE` if device changed and amount $> \$50,000$.
3. **Redis Baseline Update**: Updates Redis rolling average (Exponential Moving Average with weight $0.3$), `last_location`, `last_location_time`, and `last_device` **after** rule evaluation so the transaction does not skew its own baseline.

### Step 3: Layer 2 Machine Learning Scoring (`RestMlScorer.java`)
1. **Feature Assembly**: `RestMlScorer` builds feature vector (`amount`, `hour_of_day`, `ratio_to_avg`, `isHighRiskMerchant`, `isNewDevice`).
2. **REST Microservice Call**: Sends `POST http://localhost:5000/score` to Python FastAPI ML service.
3. **Model Prediction**: Python FastAPI service loads Scikit-Learn `RandomForestClassifier` (`fraud_model.joblib`) and calculates `fraudProbability`.
4. **Resilient Fallback**: If Python FastAPI service is offline, `RestMlScorer` catches the timeout and falls back to `StubMlScorer` heuristics.

### Step 4: 3-Tier Risk Classification & Settlement (`FraudConsumer.java`)
1. **Risk Tiering Logic**:
   * **`BLOCKED`**: `mlScore >= 0.75` OR critical rules (`AMOUNT_EXCEEDS_ABSOLUTE_LIMIT`, `IMPOSSIBLE_TRAVEL_SPEED_EXCEEDED`).
   * **`REVIEW`**: `mlScore >= 0.30` OR mild rules (`VELOCITY_LIMIT_EXCEEDED`, `NEW_DEVICE_HIGH_VALUE`, `HIGH_RISK_MERCHANT_CATEGORY`, `AMOUNT_SPIKE_VS_USER_AVERAGE`).
   * **`APPROVED`**: Clean transaction (`reasons.isEmpty()` AND `mlScore < 0.30`).
2. **Audit Logging & Event Publishing**:
   * **If `BLOCKED` or `REVIEW`**: Saves a `FraudAlert` audit entity to PostgreSQL `fraud_alerts` table and publishes the `FraudAlert` JSON object to Kafka topic `"fraud-alerts"`.
   * **If `APPROVED`**: Publishes the clean `transactionId` string to Kafka topic `"approved"`.

### Step 5: Asynchronous Status Settlement (`VerdictListener.java`)
1. **Kafka Listener**: `VerdictListener` in `payment-service` listens on topics `"approved"` and `"fraud-alerts"`.
2. **Payload Parsing**:
   * On `"approved"` topic: Strips JSON double-quotes from `transactionId`.
   * On `"fraud-alerts"` topic: Parses `verdict` (`BLOCKED` vs `REVIEW`) and `transactionId` from `FraudAlert` JSON.
3. **Final Database Update**: Looks up transaction in PostgreSQL by `transactionId` and updates `status` from `PENDING` to `APPROVED`, `REVIEW`, or `BLOCKED`.

---

## 🌟 Key Capabilities

1. **Geospatial Haversine Travel Speed Analysis**: Computes distance in kilometers between consecutive transaction coordinates/cities and flags impossible travel speeds ($> 800	ext{ km/h}$).
2. **Exponential Moving Average (EMA)**: Tracks individual user spending baselines in Redis and flags sudden spikes ($> 5	imes$ rolling average).
3. **Sliding Window Velocity Tracking**: Uses Redis `INCR` + TTL atomic counters to detect high-frequency card testing ($> 4	ext{ txns/min}$).
4. **High-Risk Merchant Filtering**: Detects transactions involving high-risk categories (Crypto exchanges, Casinos, Wire transfers, Gift cards).
5. **Python ML Microservice (FastAPI + Scikit-Learn)**: Serves a trained `RandomForestClassifier` for risk probability scoring over REST.
6. **3-Tier Decision Engine**: Categorizes transactions into `APPROVED`, `REVIEW` (step-up authentication / analyst review queue), or `BLOCKED`.
7. **Strict Message Ordering**: Messages are partitioned by `userId` in Kafka, ensuring per-user events are processed in strict chronological sequence.

---

## 🛠️ Prerequisites

- **Java 17+** & **Maven**
- **Python 3.9+**
- **Docker** & **Docker Compose**

---

## 🚀 How to Run

### 1. Start Infrastructure Containers
```bash
docker compose up -d
```
*Starts Kafka, Zookeeper, Redis, PostgreSQL (`frauddb`), and Kafka UI (`http://localhost:8090`).*

### 2. Start Python ML Microservice (Optional)
```bash
cd ml-training
python3 -m venv venv
source venv/bin/activate
pip install fastapi uvicorn pydantic joblib scikit-learn
python3 train_model.py
uvicorn ml_service:app --port 5000
```
*(If the ML service is not started, `fraud-detection-service` automatically falls back to heuristic scoring).*

### 3. Start Java Microservices

* **Terminal 1 (`payment-service`)**:
  ```bash
  cd payment-service
  mvn spring-boot:run
  ```

* **Terminal 2 (`fraud-detection-service`)**:
  ```bash
  cd fraud-detection-service
  mvn spring-boot:run
  ```

---

## 🧪 Test Suite

Run these `curl` commands to test all fraud rules and 3-tier verdicts:

### 1. Normal Transaction (`APPROVED`)
```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"userId":"u1","amount":500,"merchant":"Amazon","deviceId":"dev-1","location":"IN-PUNE"}'
```

### 2. Absolute High Amount Limit (`BLOCKED`)
```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"userId":"u3","amount":250000,"merchant":"Luxury Store","deviceId":"dev-3","location":"IN-MUMBAI"}'
```

### 3. Impossible Travel Speed (`BLOCKED`)
```bash
# Step A: Transaction in Pune
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"userId":"u10","amount":200,"merchant":"Starbucks","deviceId":"dev-10","location":"IN-PUNE"}'

# Step B: Transaction in London 2 seconds later (7,200 km away)
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"userId":"u10","amount":300,"merchant":"Duty Free","deviceId":"dev-10","location":"UK-LON"}'
```

### 4. High-Risk Merchant Category (`REVIEW`)
```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"userId":"u5","amount":5000,"merchant":"CRYPTO_EXCHANGE_GLOBAL","deviceId":"dev-5","location":"IN-DELHI"}'
```

### 5. Rapid Velocity Check (`REVIEW`)
```bash
for i in {1..6}; do curl -s -X POST http://localhost:8081/api/v1/transactions -H "Content-Type: application/json" -d '{"userId":"u2","amount":100,"merchant":"Test","deviceId":"dev-2","location":"IN-PUNE"}'; done
```

---

## 🔍 Inspection & Monitoring

* **Inspect Audit Alerts**: `curl http://localhost:8082/api/v1/alerts`
* **Check Transaction Status**: `curl http://localhost:8081/api/v1/transactions/{transactionId}`
* **Kafka UI Dashboard**: Open [http://localhost:8090](http://localhost:8090) in browser to monitor Kafka topics live.

---

## 📄 License
MIT
