# Verifica Boleto

<div align="center">

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/Python_3.10-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Flask](https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white)
![scikit-learn](https://img.shields.io/badge/scikit--learn-F7931E?style=for-the-badge&logo=scikit-learn&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Render](https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=black)
![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)


<p>
  <a href="https://frontend-verificaboleto.onrender.com/">🌐 Acessar o sistema </a> •
  <a href="https://verificaboleto.onrender.com/">🌐 Acessar o sistema</a> •
  <a href="https://verificaboleto.onrender.com/swagger-ui/index.html">📖 Documentação da API</a>
</p>

</div>


> **Sistema web de detecção de fraudes em boletos bancários com análise híbrida: regras fixas + Machine Learning (Random Forest).**

Boletos adulterados são uma das fraudes financeiras mais comuns no Brasil. O **VerificaBoleto** combina validação determinística com aprendizado de máquina para classificar qualquer boleto recebido como **✅ Seguro**, **⚠️ Suspeito** ou **🚨 Fraude**, com score de risco e explicação detalhada.

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
- **Score de risco** — cada análise retorna um score entre 0 e 100, permitindo decisões graduais 
- **Perfil do beneficiário** — considera reputação (0–100), histórico de fraudes e tempo de existência da empresa
- **API REST documentada** — integração simples via `POST /boletos/analise`

---

## Arquitetura

O sistema é composto por **duas aplicações independentes** que se comunicam via HTTP:

```
┌─────────────────────┐
│   Frontend Web      │  HTML / CSS / JS
│   (Render)          │
└────────┬────────────┘
         │ HTTP
┌────────▼────────────┐        ┌─────────────────────┐
│   Spring Boot       │ REST   │   Python Flask      │
│   API (porta 8080)  │◄──────►│   ML (porta 5000)   │
│   (Render)          │        │   (Render)          │
└────────┬────────────┘        └─────────────────────┘
         │ JPA
┌────────▼────────────┐
│   PostgreSQL        │
│   (Render)          │
└─────────────────────┘

```

---

## Fluxo de Análise

A análise ocorre em **duas camadas sequenciais**:

###  Regras Fixas
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

###  Machine Learning
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
VerificaBoleto/
├── src/main/java/com/A3/verifica_boleto/
│   ├── config/          AppConfig.java (bean RestTemplate)
│   ├── controller/      BoletoController, PdfController, ChatController
│   ├── model/           Boleto, Beneficiario
│   ├── repository/      BoletoRepository, BeneficiarioRepository
│   ├── service/         BoletoService, BeneficiarioService,
│   │                    MlFraudService, PdfService, ChatbotService
│   └── util/            LinhaDigitavelParser
├── python ML/
│   ├── app_flask.py          microserviço Flask
│   └── treino_e_modelo.py    script de treinamento do modelo
├── Dockerfile
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
# 1. Clone o repositório
git clone https://github.com/seu-usuario/verifica-boleto.git
cd verifica-boleto

# 2. Configure as credenciais
spring.datasource.url=jdbc:postgresql://localhost:5432/verifica_boleto
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
gemini.api.key=sua_chave_gemini

# 3. Treine o modelo ML e suba o Python
cd "python ML"
pip install flask pandas scikit-learn
python treino_e_modelo.py
python app_flask.py
# Servidor ML rodando em http://localhost:5000

# 4. Suba o Spring Boot
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# API rodando em http://localhost:8080

# 5. Acesse
http://localhost:8080/swagger-ui/index.html

```

---

## O Modelo de Machine Learning

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

## Desenvolvedoras

| Nome | RA |
|---|---|
| Geovanna Gabriela Pessoa de Jesus | 12524238308 |
| Thais Lopes Barbosa | 12525151004 |

Projeto A3 — Sistemas Distribuidos e Mobile — Universidade Anhembi Morumbi — 2026

---

## 📄 Licença

Distribuído sob a licença **MIT**. Consulte o arquivo [LICENSE](LICENSE) para mais informações.

---

<div align="center">
  Feito com ☕ e muito <code>System.out.println()</code> para depurar
</div>