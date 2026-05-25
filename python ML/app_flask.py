"""
Microserviço de detecção de fraudes em boletos. v3
"""

from flask import Flask, request, jsonify
import pickle
import pandas as pd
import os

app = Flask(__name__)

MODEL_PATH = os.path.join(os.path.dirname(__file__), "modelo_fraude.pkl")
with open(MODEL_PATH, "rb") as f:
    model = pickle.load(f)

FEATURES = [
    "desvio_valor_signed",    # com sinal: negativo = recebido menor que banco (golpe clássico)
    "desvio_valor_abs",       # diferença absoluta em reais
    "desvio_valor_pct",       # diferença percentual
    "diff_vencimento_dias",
    "cnpj_divergente",
    "banco_divergente",
    "reputacao_score",
    "historico_fraude",
    "dias_desde_abertura",
]

def calcular_score_risco(resultado, score_fraude, score_suspeito, score_seguro):
    """
    Normaliza os scores do ML para uma escala intuitiva de 0 a 100:
      0  – 30  → Seguro
      31 – 69  → Suspeito
      70 – 100 → Fraude
    """
    if resultado == "fraude":
        # Mapeia scoreFraude (0.75–1.0) para faixa 70–100
        score_risco = 70 + int((score_fraude - 0.75) / 0.25 * 30)
        return max(70, min(100, score_risco))
    elif resultado == "suspeito":
        # Mapeia scoreSuspeito (0.50–1.0) para faixa 31–69
        score_risco = 31 + int((score_suspeito - 0.50) / 0.50 * 38)
        return max(31, min(69, score_risco))
    else:
        # Mapeia scoreSeguro (0.70–1.0) para faixa 0–30 (invertido)
        score_risco = 30 - int((score_seguro - 0.70) / 0.30 * 30)
        return max(0, min(30, score_risco))


@app.route("/analisar", methods=["POST"])
def analisar():
    dados = request.get_json()
    if not dados:
        return jsonify({"erro": "Body JSON ausente"}), 400

    ausentes = [f for f in FEATURES if f not in dados]
    if ausentes:
        return jsonify({"erro": f"Campos ausentes: {ausentes}"}), 400

    X = pd.DataFrame([{f: dados[f] for f in FEATURES}])
    resultado     = model.predict(X)[0]
    probabilidades = dict(zip(model.classes_, model.predict_proba(X)[0]))

    score_fraude   = round(probabilidades.get("fraude",   0), 4)
    score_suspeito = round(probabilidades.get("suspeito", 0), 4)
    score_seguro   = round(probabilidades.get("seguro",   0), 4)

    score_risco = calcular_score_risco(resultado, score_fraude, score_suspeito, score_seguro)

    return jsonify({
        "resultado":      resultado,
        "score_risco":    score_risco,       # 0–100, pronto para o frontend
        "score_fraude":   score_fraude,
        "score_suspeito": score_suspeito,
        "score_seguro":   score_seguro,
    })


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "modelo": "modelo_fraude.pkl"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)