package com.A3.verifica_boleto.service;

import com.A3.verifica_boleto.model.Beneficiario;
import com.A3.verifica_boleto.model.Boleto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class MlFraudService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    public MlResultado analisar(Boleto boletoRecebido, Boleto boletoDoBanco) {

        // Desvio com sinal:
        // negativo = recebido MENOR que banco → golpe clássico (imprime valor menor)
        // positivo = recebido MAIOR que banco → também suspeito
        BigDecimal diff = boletoRecebido.getValor().subtract(boletoDoBanco.getValor());

        double desvioValorSigned = diff.setScale(2, RoundingMode.HALF_UP).doubleValue();
        double desvioValorAbs    = diff.abs().setScale(2, RoundingMode.HALF_UP).doubleValue();
        double desvioValorPct    = 0.0;

        if (boletoDoBanco.getValor().compareTo(BigDecimal.ZERO) != 0) {
            desvioValorPct = diff.abs()
                    .divide(boletoDoBanco.getValor(), 10, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        long diffVencimentoDias = Math.abs(ChronoUnit.DAYS.between(
                boletoRecebido.getDataVencimento(),
                boletoDoBanco.getDataVencimento()
        ));

        int cnpjDivergente  = boletoRecebido.getBeneficiario().getCnpj()
                .equals(boletoDoBanco.getBeneficiario().getCnpj()) ? 0 : 1;
        int bancoDivergente = boletoRecebido.getBancoEmissor()
                .equals(boletoDoBanco.getBancoEmissor()) ? 0 : 1;

        Beneficiario benDB      = boletoDoBanco.getBeneficiario();
        long diasDesdeAbertura  = ChronoUnit.DAYS.between(benDB.getDataAbertura(), LocalDate.now());

        Map<String, Object> payload = new HashMap<>();
        payload.put("desvio_valor_signed",   desvioValorSigned);
        payload.put("desvio_valor_abs",      desvioValorAbs);
        payload.put("desvio_valor_pct",      desvioValorPct);
        payload.put("diff_vencimento_dias",  diffVencimentoDias);
        payload.put("cnpj_divergente",       cnpjDivergente);
        payload.put("banco_divergente",      bancoDivergente);
        payload.put("reputacao_score",       benDB.getReputacaoScore());
        payload.put("historico_fraude",      benDB.getHistoricoFraude());
        payload.put("dias_desde_abertura",   diasDesdeAbertura);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    mlServiceUrl + "/analisar", payload, Map.class);

            if (response == null) return MlResultado.fallback("Resposta nula");

            return new MlResultado(
                    (String) response.get("resultado"),
                    toInt(response.get("score_risco")),
                    toDouble(response.get("score_fraude")),
                    toDouble(response.get("score_suspeito")),
                    toDouble(response.get("score_seguro")),
                    null
            );
        } catch (Exception e) {
            return MlResultado.fallback("Serviço ML indisponível: " + e.getMessage());
        }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        return ((Number) val).doubleValue();
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        return ((Number) val).intValue();
    }

    public static class MlResultado {
        public final String resultado;
        public final int    scoreRisco;     // 0–100 normalizado
        public final double scoreFraude;
        public final double scoreSuspeito;
        public final double scoreSeguro;
        public final String erro;

        public MlResultado(String resultado, int scoreRisco,
                           double scoreFraude, double scoreSuspeito,
                           double scoreSeguro, String erro) {
            this.resultado     = resultado;
            this.scoreRisco    = scoreRisco;
            this.scoreFraude   = scoreFraude;
            this.scoreSuspeito = scoreSuspeito;
            this.scoreSeguro   = scoreSeguro;
            this.erro          = erro;
        }

        public boolean isDisponivel() { return erro == null; }

        public static MlResultado fallback(String motivo) {
            return new MlResultado("indisponivel", 0, 0, 0, 0, motivo);
        }
    }
}