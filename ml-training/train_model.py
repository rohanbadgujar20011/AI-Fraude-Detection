import os
import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score

def generate_synthetic_fraud_dataset(n_samples=5000, fraud_ratio=0.03):
    np.random.seed(42)
    n_fraud = int(n_samples * fraud_ratio)
    n_normal = n_samples - n_fraud

    # Normal Transactions
    normal_amounts = np.random.exponential(scale=100, size=n_normal) + 5
    normal_hours = np.random.randint(6, 23, size=n_normal)
    normal_ratios = np.random.normal(loc=1.0, scale=0.5, size=n_normal)
    normal_ratios = np.clip(normal_ratios, 0.1, 3.0)
    normal_high_risk_merchant = np.random.choice([0, 1], size=n_normal, p=[0.95, 0.05])
    normal_new_device = np.random.choice([0, 1], size=n_normal, p=[0.90, 0.10])
    y_normal = np.zeros(n_normal)

    # Fraudulent Transactions
    fraud_amounts = np.random.exponential(scale=2000, size=n_fraud) + 500
    fraud_hours = np.random.choice([1, 2, 3, 4, 23, 0], size=n_fraud)
    fraud_ratios = np.random.uniform(5.0, 20.0, size=n_fraud)
    fraud_high_risk_merchant = np.random.choice([0, 1], size=n_fraud, p=[0.40, 0.60])
    fraud_new_device = np.random.choice([0, 1], size=n_fraud, p=[0.30, 0.70])
    y_fraud = np.ones(n_fraud)

    # Feature Matrix: [amount, hour, ratio, high_risk_merchant, new_device]
    X_normal = np.column_stack([normal_amounts, normal_hours, normal_ratios, normal_high_risk_merchant, normal_new_device])
    X_fraud = np.column_stack([fraud_amounts, fraud_hours, fraud_ratios, fraud_high_risk_merchant, fraud_new_device])

    X = np.vstack([X_normal, X_fraud])
    y = np.concatenate([y_normal, y_fraud])

    return X, y

def main():
    print("Generating synthetic fraud training data...")
    X, y = generate_synthetic_fraud_dataset(n_samples=10000, fraud_ratio=0.04)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    print("Training Random Forest Classifier on fraud dataset...")
    clf = RandomForestClassifier(n_estimators=100, max_depth=10, random_state=42)
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)
    y_prob = clf.predict_proba(X_test)[:, 1]

    print("")
    print("=== Model Evaluation ===")
    print(classification_report(y_test, y_pred, target_names=["Legitimate", "Fraud"]))
    print("ROC-AUC Score: {:.4f}".format(roc_auc_score(y_test, y_prob)))

    model_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(model_dir, "fraud_model.joblib")
    joblib.dump(clf, model_path)
    print("")
    print("Model saved successfully to '{}'!".format(model_path))

if __name__ == "__main__":
    main()
