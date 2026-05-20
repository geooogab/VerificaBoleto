# VerificaBoleto

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/Python_3.10-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Flask](https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white)
![scikit-learn](https://img.shields.io/badge/scikit--learn-F7931E?style=for-the-badge&logo=scikit-learn&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

> **Sistema web de detecção de fraudes em boletos bancários com análise híbrida: regras fixas + Machine Learning (Random Forest).**

Boletos adulterados são uma das fraudes financeiras mais comuns no Brasil. O **VerificaBoleto** combina validação determinística com aprendizado de máquina para classificar qualquer boleto recebido como **✅ Seguro**, **⚠️ Suspeito** ou **🚨 Fraude** — com score de risco e explicação detalhada.

---

## Índice

- [Funcionalidades](#-funcionalidades)
- [Arquitetura](#-arquitetura)
- [Fluxo de Análise](#-fluxo-de-análise)
- [Stack de Tecnologias](#-stack-de-tecnologias)
- [Estrutura de Pastas](#-estrutura-de-pastas)
- [Como Executar](#-como-executar)
- [Exemplos de Uso (API)](#-exemplos-de-uso-api)
- [O Modelo de Machine Learning](#-o-modelo-de-machine-learning)
- [Informações Acadêmicas](#-informações-acadêmicas)
- [Licença](#-licença)

---

## Funcionalidades

- **Verificação campo a campo** — compara valor, vencimento, banco, CNPJ e razão social com o boleto oficial cadastrado no sistema
- **Classificação via ML** — modelo Random Forest avalia desvios numéricos e retorna probabilidades por classe
- **Fallback resiliente** — se o microserviço Python estiver indisponível, o sistema continua operando pelas regras fixas
- **Score de risco** — cada análise retorna um score entre 0 e 1, permitindo decisões graduais (não apenas binário)
- **Perfil do beneficiário** — considera reputação (0–100), histórico de fraudes e tempo de existência da empresa
- **API REST documentada** — integração simples via `POST /boletos/analise`

---

## Arquitetura

O sistema é composto por **duas aplicações independentes** que se comunicam via HTTP:

```
┌─────────────────────────────────────────────────────────┐
│                      USUÁRIO / FRONTEND                  │
│                   HTML · CSS · JavaScript                │
└─────────────────────────┬───────────────────────────────┘
                          │  POST /boletos/analise
                          ▼
┌─────────────────────────────────────────────────────────┐
│              SPRING BOOT                                │
│                                                         │
│  Controller ──► Service ──► Repository (Banco de Dados) │
│                    │                                    │
│                    │  Calcula desvios numéricos         │
│                    ▼                                    │
│              ┌──────────┐                               │
│              │  Python  │  POST                         │
│              │  Flask   │◄──────────────────────────────┤
│              │          │                               │
│              └──────────┘                               │
│                    │  { probabilidades por classe }     │
│                    ▼                                    │
│            Monta resposta final                         │
│     { status, scoreRisco, verificacoes, detalhe }       │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
               Resposta JSON ao usuário
```

---

## Fluxo de Análise

A análise ocorre em **três camadas sequenciais**:

### Camada 1 — Regras Fixas
Validação determinística campo a campo contra o boleto oficial:

| Campo verificado | Tipo de comparação |
|---|---|
| Valor | Igualdade / desvio percentual |
| Data de vencimento | Diferença em dias |
| Banco emissor | Igualdade binária |
| CNPJ do beneficiário | Igualdade binária |
| Razão social | Correspondência textual |
| Reputação do beneficiário | Score 0–100 |
| Histórico de fraudes | Booleano |
| Idade da empresa | Dias desde abertura |

### Camada 2 — Machine Learning
O Java serializa os desvios calculados e envia ao microserviço Python:

```json
{
  "desvioPercentualValor": 0.42,
  "diferencaDiasVencimento": 0,
  "divergenciaCNPJ": false,
  "divergenciaBanco": false,
  "scoreReputacao": 87,
  "historicoDeFraudes": false,
  "diasDesdeAbertura": 2190
}
```

O modelo Random Forest pondera esses features e retorna a probabilidade de cada classe. Um desvio de **2% no valor** é tratado de forma completamente diferente de um desvio de **500%**.

### Camada 3 — Fallback
Se o microserviço Python estiver indisponível (timeout ou erro de conexão), o sistema retorna o resultado das **regras fixas** com `"origem": "Regras"` — garantindo disponibilidade contínua.

---

## Stack de Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend principal | Java 17 + Spring Boot |
| Frontend | HTML, CSS, JavaScript |
| Banco de dados | PostgreSQL (prod) |
| Microserviço de ML | Python 3.10 + Flask |
| Modelo de ML | scikit-learn — Random Forest |

---

## Estrutura de Pastas

```
verifica-boleto/
│
├── src/                             # Aplicação Spring Boot
│   └── main/
│       ├── java/com/verificaboleto/
│       │   ├── controller/          # Endpoints REST
│       │   ├── service/             # Regras de negócio e orquestração
│       │   ├── model/               # Entidades JPA
│       │   ├── repository/          # Acesso ao banco de dados
│       │   ├── util/                # Utilitários (cálculo de desvios, etc.)
│       │   └── config/              # Configurações do Spring
│       └── resources/
│           └── application.properties
│
├── python-ml/                       # Microserviço Python
│   ├── app_flask.py                 # API Flask (porta 5000)
│   ├── gerar_treino_e_modelo.py     # Script de treinamento do Random Forest
│   ├── modelo_rf.pkl                # Modelo serializado (gerado pelo script)
│   └── requirements.txt
│
├── frontend/                        # Interface web
│   ├── index.html
│   ├── style.css
│   └── script.js
│
├── pom.xml
├── mvnw
└── README.md
```

---

## Como Executar

### Pré-requisitos

- Java 17+
- Python 3.10+
- Maven 3.8+
- pip

### Passo a passo

```bash
# 1. Clonar o repositório
git clone https://github.com/seu-usuario/verifica-boleto.git
cd verifica-boleto

# 2. Instalar dependências Python
pip install flask pandas scikit-learn

# 3. Gerar o modelo de ML (necessário apenas na primeira execução)
cd python-ml
python treino_e_modelo.py

# 4. Subir o microserviço Python (mantenha este terminal aberto)
python app_flask.py
# Microserviço rodando em http://localhost:5000

# 5. Em outro terminal, subir o Spring Boot
cd ..
./mvnw spring-boot:run
# API principal rodando em http://localhost:8080
```

> **Dica:** O microserviço Python deve ser iniciado **antes** do Spring Boot para que a integração ML esteja disponível desde o início. Caso contrário, o sistema opera em modo fallback (regras fixas).

---

## 📡 Exemplos de Uso (API)

### Request

```http
POST /boletos/analise
Content-Type: application/json
```

```json
{
  "codigoBarras": "34191.75124 34567.261229 68055.320001 1 92380000025000",
  "valor": 297.50,
  "vencimento": "2025-06-15",
  "bancoCodigo": "341",
  "cnpjBeneficiario": "12.345.678/0001-99",
  "razaoSocialBeneficiario": "Empresa Exemplo Ltda"
}
```

### Response — Fraude detectada

```json
{
  "status": "Fraude",
  "origem": "ML",
  "scoreRisco": 0.92,
  "verificacoes": [
    { "nome": "Valor confere com o banco",   "ok": false },
    { "nome": "Data de vencimento confere",  "ok": true  },
    { "nome": "Banco emissor confere",       "ok": true  },
    { "nome": "CNPJ do beneficiário válido", "ok": true  },
    { "nome": "Razão social confere",        "ok": true  }
  ],
  "detalhe": "Valor divergente em 485%. ML classificou como fraude com alta confiança."
}
```

### Response — Boleto seguro

```json
{
  "status": "Seguro",
  "origem": "ML",
  "scoreRisco": 0.04,
  "verificacoes": [
    { "nome": "Valor confere com o banco",   "ok": true },
    { "nome": "Data de vencimento confere",  "ok": true },
    { "nome": "Banco emissor confere",       "ok": true },
    { "nome": "CNPJ do beneficiário válido", "ok": true },
    { "nome": "Razão social confere",        "ok": true }
  ],
  "detalhe": "Todos os campos conferem. Nenhuma anomalia detectada."
}
```

### Campos de `status` possíveis

| Status | scoreRisco | Significado |
|---|---|---|
| `Seguro` | 0.00 – 0.30 | Boleto legítimo, pode pagar |
| `Suspeito` | 0.31 – 0.69 | Requer atenção — verificar manualmente |
| `Fraude` | 0.70 – 1.00 | Alta probabilidade de fraude |

---

## 🤖 O Modelo de Machine Learning

### O que é Random Forest?

Random Forest é um algoritmo de **aprendizado de máquina por ensemble**: ele treina centenas de árvores de decisão independentes e combina seus resultados por votação para produzir uma predição mais robusta e menos suscetível a overfitting do que uma árvore única.

```
Feature 1 ──┐
Feature 2 ──┤──► Árvore 1 ──┐
Feature 3 ──┤                │
            │    Árvore 2 ──┤──► Votação ──► Classe + Probabilidade
Feature N ──┤                │
                 ...        │
             Árvore 200 ──┘
```

### Por que Random Forest foi escolhido?

| Critério | Justificativa |
|---|---|
| **Lida bem com features mistas** | O modelo recebe tanto valores contínuos (desvio %) quanto binários (CNPJ diverge?) sem pré-processamento complexo |
| **Robusto a outliers** | Boletos fraudulentos podem ter desvios extremos — o RF não é distorcido por eles |
| **Fornece probabilidades** | Permite o score de risco contínuo (0–1), não apenas classificação binária |
| **Interpretabilidade** | Feature importance mostra quais variáveis mais influenciam a detecção |
| **Sem necessidade de normalização** | Facilita a integração com os desvios calculados pelo Java |

### Features utilizadas no treinamento

| Feature | Tipo | Descrição |
|---|---|---|
| `desvioPercentualValor` | Float | Quanto o valor diverge do oficial (%) |
| `diferencaDiasVencimento` | Int | Diferença em dias no vencimento |
| `divergenciaCNPJ` | Bool | CNPJ não corresponde ao beneficiário |
| `divergenciaBanco` | Bool | Banco emissor diverge |
| `scoreReputacao` | Int 0–100 | Reputação histórica do beneficiário |
| `historicoDeFraudes` | Bool | Beneficiário já foi reportado |
| `diasDesdeAbertura` | Int | Idade da empresa em dias |

O modelo foi treinado com **200 árvores de decisão** (`n_estimators=200`) via `scikit-learn`.

---

## 🎓 Informações Acadêmicas

| Campo | Informação |
|---|---|
| Disciplina | A3 — Atividade de Avaliação Integrada |
| Curso | Ciência da Computação |
| Requisito atendido | Obrigatoriedade de uso de Inteligência Artificial |
| Implementação de IA | Modelo Random Forest em Python integrado via microserviço Flask ao backend Java |

---

## 📄 Licença

Distribuído sob a licença **MIT**. Consulte o arquivo [LICENSE](LICENSE) para mais informações.

---

<div align="center">
  Feito com ☕ e muito <code>System.out.println()</code> para depurar
</div>