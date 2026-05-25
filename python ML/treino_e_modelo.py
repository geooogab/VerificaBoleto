"""
Gerador de dados sintéticos + treinamento do modelo de detecção de fraudes em boletos.
v3 — desvio com sinal (negativo = recebido menor que banco = golpe clássico)
     normalização do score para faixa 0-100
"""

import pandas as pd
import numpy as np
import pickle
import json
from datetime import date
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import warnings
warnings.filterwarnings("ignore")

np.random.seed(42)
N = 800

beneficiarios = [
    {"id": 1,  "reputacao": 30,  "historico_fraude": 0, "dias_desde_abertura": (date.today() - date(2026, 1, 12)).days},
    {"id": 2,  "reputacao": 85,  "historico_fraude": 0, "dias_desde_abertura": (date.today() - date(2023, 9, 1)).days},
    {"id": 3,  "reputacao": 95,  "historico_fraude": 0, "dias_desde_abertura": (date.today() - date(2020, 1, 20)).days},
    {"id": 4,  "reputacao": 90,  "historico_fraude": 0, "dias_desde_abertura": (date.today() - date(2018, 7, 15)).days},
    {"id": 5,  "reputacao": 100, "historico_fraude": 1, "dias_desde_abertura": (date.today() - date(2012, 3, 10)).days},
    {"id": 6,  "reputacao": 10,  "historico_fraude": 1, "dias_desde_abertura": (date.today() - date(2026, 3, 25)).days},
    {"id": 7,  "reputacao": 40,  "historico_fraude": 3, "dias_desde_abertura": (date.today() - date(2026, 2, 14)).days},
    {"id": 8,  "reputacao": 88,  "historico_fraude": 3, "dias_desde_abertura": (date.today() - date(2017, 6, 30)).days},
    {"id": 9,  "reputacao": 0,   "historico_fraude": 3, "dias_desde_abertura": (date.today() - date(2021, 9, 5)).days},
    {"id": 10, "reputacao": 5,   "historico_fraude": 3, "dias_desde_abertura": (date.today() - date(2024, 12, 1)).days},
]

registros = []

for i in range(N):
    b = beneficiarios[np.random.randint(0, len(beneficiarios))]

    valor_base = round(np.random.uniform(50, 10000), 2)

    # desvio_valor_signed:
    #   negativo = recebido MENOR que banco (golpe clássico — maior risco)
    #   positivo = recebido MAIOR que banco (também suspeito)
    #   zero     = igual
    tipo = np.random.choice(
        ["sem_desvio", "centavos_neg", "centavos_pos", "pequeno_neg", "pequeno_pos", "alto_neg", "alto_pos"],
        p=[0.40, 0.05, 0.05, 0.20, 0.10, 0.15, 0.05]
    )

    if tipo == "sem_desvio":
        desvio_valor_signed = 0.0
    elif tipo == "centavos_neg":
        desvio_valor_signed = -round(np.random.uniform(0.01, 0.99), 2)
    elif tipo == "centavos_pos":
        desvio_valor_signed = round(np.random.uniform(0.01, 0.99), 2)
    elif tipo == "pequeno_neg":
        desvio_valor_signed = -round(np.random.uniform(1.0, 50.0), 2)
    elif tipo == "pequeno_pos":
        desvio_valor_signed = round(np.random.uniform(1.0, 50.0), 2)
    elif tipo == "alto_neg":
        desvio_valor_signed = -round(np.random.uniform(50.01, valor_base * 0.9), 2)
    else:
        desvio_valor_signed = round(np.random.uniform(50.01, valor_base * 5), 2)

    desvio_valor_abs = abs(desvio_valor_signed)
    desvio_valor_pct = round(desvio_valor_abs / valor_base, 6) if valor_base > 0 else 0.0

    tipo_data = np.random.choice(["igual", "um_dia", "poucos", "muitos"], p=[0.55, 0.20, 0.15, 0.10])
    if tipo_data == "igual":
        diff_vencimento_dias = 0
    elif tipo_data == "um_dia":
        diff_vencimento_dias = 1
    elif tipo_data == "poucos":
        diff_vencimento_dias = np.random.randint(2, 10)
    else:
        diff_vencimento_dias = np.random.randint(10, 90)

    cnpj_divergente  = int(np.random.random() < 0.15)
    banco_divergente = int(np.random.random() < 0.10)
    reputacao        = b["reputacao"]
    historico_fraude = b["historico_fraude"]
    dias_abertura    = b["dias_desde_abertura"]

    # ── Rotulagem ─────────────────────────────────────────────────────────────
    score = 0.0

    # Valor com sinal — negativo (recebido menor) é o golpe clássico, peso maior
    if desvio_valor_signed == 0:
        score += 0.0
    elif desvio_valor_signed < 0:
        # recebido MENOR que banco — golpe clássico
        abs_dev = abs(desvio_valor_signed)
        if abs_dev < 1.0:
            score += 0.10
        elif abs_dev <= 10.0:
            score += 0.60   # suspeito grave
        elif abs_dev <= 50.0:
            score += 0.80   # fraude provável
        else:
            score += 1.00   # fraude clara
    else:
        # recebido MAIOR que banco — também suspeito, peso menor
        if desvio_valor_signed < 1.0:
            score += 0.05
        elif desvio_valor_signed <= 10.0:
            score += 0.40
        elif desvio_valor_signed <= 50.0:
            score += 0.60
        else:
            score += 0.85

    # Vencimento
    if diff_vencimento_dias == 0:
        score += 0.0
    elif diff_vencimento_dias == 1:
        score += 0.05
    elif diff_vencimento_dias <= 5:
        score += 0.20
    else:
        score += 0.45

    if cnpj_divergente:  score += 0.70
    if banco_divergente: score += 0.30

    if reputacao >= 80:   score -= 0.10
    elif reputacao >= 50: score += 0.10
    elif reputacao >= 25: score += 0.30
    else:                 score += 0.55

    score += min(historico_fraude * 0.25, 0.75)

    if dias_abertura < 30:   score += 0.40
    elif dias_abertura < 90: score += 0.20

    score = np.clip(score + np.random.normal(0, 0.04), 0, 3.0)

    if score < 0.35:
        label = "seguro"
    elif score < 0.80:
        label = "suspeito"
    else:
        label = "fraude"

    registros.append({
        "desvio_valor_signed":   desvio_valor_signed,
        "desvio_valor_abs":      desvio_valor_abs,
        "desvio_valor_pct":      desvio_valor_pct,
        "diff_vencimento_dias":  diff_vencimento_dias,
        "cnpj_divergente":       cnpj_divergente,
        "banco_divergente":      banco_divergente,
        "reputacao_score":       reputacao,
        "historico_fraude":      historico_fraude,
        "dias_desde_abertura":   dias_abertura,
        "label":                 label,
    })

df = pd.DataFrame(registros)

print("=== Distribuição das classes ===")
print(df["label"].value_counts())
print()

FEATURES = [
    "desvio_valor_signed",
    "desvio_valor_abs",
    "desvio_valor_pct",
    "diff_vencimento_dias",
    "cnpj_divergente",
    "banco_divergente",
    "reputacao_score",
    "historico_fraude",
    "dias_desde_abertura",
]

X = df[FEATURES]
y = df["label"]

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, stratify=y, random_state=42)
model = RandomForestClassifier(n_estimators=200, max_depth=10, class_weight="balanced", random_state=42)
model.fit(X_train, y_train)

print("=== Relatório de classificação ===")
print(classification_report(model.predict(X_test), y_test))

print("=== Importância das features ===")
for feat, imp in sorted(zip(FEATURES, model.feature_importances_), key=lambda x: -x[1]):
    print(f"  {feat:<28} {'█' * int(imp * 40)} {imp:.3f}")
print()

with open("modelo_fraude.pkl", "wb") as f:
    pickle.dump(model, f)
with open("modelo_metadata.json", "w") as f:
    json.dump({"features": FEATURES, "classes": list(model.classes_)}, f, ensure_ascii=False, indent=2)
df.to_csv("dataset_treino.csv", index=False)

# ── Testes ────────────────────────────────────────────────────────────────────
casos = [
    ("Seguro — sem desvio",
     {"desvio_valor_signed": 0.0,    "desvio_valor_abs": 0.0,   "desvio_valor_pct": 0.0,
      "diff_vencimento_dias": 0, "cnpj_divergente": 0, "banco_divergente": 0,
      "reputacao_score": 95, "historico_fraude": 0, "dias_desde_abertura": 1800}),
    ("Suspeito — recebido R$2 menor (golpe)",
     {"desvio_valor_signed": -2.0,   "desvio_valor_abs": 2.0,   "desvio_valor_pct": 0.002,
      "diff_vencimento_dias": 0, "cnpj_divergente": 0, "banco_divergente": 0,
      "reputacao_score": 85, "historico_fraude": 0, "dias_desde_abertura": 900}),
    ("Fraude — recebido R$800 menor (golpe clássico)",
     {"desvio_valor_signed": -800.0, "desvio_valor_abs": 800.0, "desvio_valor_pct": 0.80,
      "diff_vencimento_dias": 0, "cnpj_divergente": 1, "banco_divergente": 0,
      "reputacao_score": 10, "historico_fraude": 2, "dias_desde_abertura": 40}),
]

for nome, feat in casos:
    ex = pd.DataFrame([feat])
    proba = dict(zip(model.classes_, model.predict_proba(ex)[0]))
    pred  = model.predict(ex)[0]
    score_fraude = proba.get("fraude", 0)
    score_risco  = int(score_fraude * 100) if pred == "fraude" else \
                   int(31 + score_fraude * 38 / 0.75) if pred == "suspeito" else \
                   int(score_fraude * 30 / 0.35)
    print(f"=== {nome} ===")
    for cls, p in proba.items(): print(f"  {cls:<10} {p:.1%}")
    print(f"  → {pred.upper()} | score_risco: {score_risco}/100")
    print()