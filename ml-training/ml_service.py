import os
import joblib
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="Fraud Detection ML Scoring Microservice")

MODEL_PATH = os.path.join(os.path.dirname(__file__), "fraud_model.joblib")

model = None
if os.path.exists(MODEL_PATH):
    try:
        model = joblib.load(MODEL_PATH)
        print(f"Loaded ML model from {MODEL_PATH}")
    except Exception as e:
        print(f"Error loading model: {e}")

class ScoreRequest(BaseModel):
    amount: float
    hour: int = 12
    ratio: float = 1.0
    isHighRiskMerchant: int = 0
    isNewDevice: int = 0

class ScoreResponse(BaseModel):
    fraudProbability: float
    verdict: str

@app.post("/score", response_model=ScoreResponse)
def score_transaction(req: ScoreRequest):
    if model is not None:
        try:
            features = np.array([[req.amount, req.hour, req.ratio, req.isHighRiskMerchant, req.isNewDevice]])
            prob = float(model.predict_proba(features)[0][1])
        except Exception as e:
            prob = heuristic_score(req)
    else:
        prob = heuristic_score(req)

    if prob >= 0.75:
        verdict = "BLOCKED"
    elif prob >= 0.30:
        verdict = "REVIEW"
    else:
        verdict = "APPROVED"

    return ScoreResponse(fraudProbability=round(prob, 4), verdict=verdict)

def heuristic_score(req: ScoreRequest) -> float:
    score = 0.05
    if req.amount > 100000:
        score += 0.60
    if req.ratio > 5.0:
        score += 0.30
    if req.hour >= 1 and req.hour <= 4:
        score += 0.20
    if req.isHighRiskMerchant == 1:
        score += 0.25
    if req.isNewDevice == 1:
        score += 0.20
    return min(score, 1.0)

@app.get("/health")
def health():
    return {"status": "UP", "modelLoaded": model is not None}
