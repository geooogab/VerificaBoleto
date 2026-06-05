package com.A3.verifica_boleto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.A3.verifica_boleto.model.Boleto;
import com.A3.verifica_boleto.service.MlFraudService.MlResultado;

import java.util.ArrayList;
import java.util.List;

@Service
public class BoletoService {

    @Autowired
    private BeneficiarioService beneficiarioService;

    @Autowired
    private MlFraudService mlFraudService;

    public static class Verificacao {
    public String nome;
    public boolean ok;
    public String valorInformado; // Novo
    public String valorBanco;     // Novo

    public Verificacao(String nome, boolean ok, String valorInformado, String valorBanco) {
        this.nome = nome;
        this.ok   = ok;
        this.valorInformado = valorInformado;
        this.valorBanco     = valorBanco;
    }
}
    public static class ResultadoAnalise {
        public String status;           // "Seguro" | "Suspeito" | "Fraude"
        public String origem;           // "ML" | "Regras fixas"
        public Integer scoreRisco;      // 0–100 (null se fallback)
        public List<Verificacao> verificacoes;
        public String detalhe;

        public ResultadoAnalise(String status, String origem, Integer scoreRisco,
                                List<Verificacao> verificacoes, String detalhe) {
            this.status       = status;
            this.origem       = origem;
            this.scoreRisco   = scoreRisco;
            this.verificacoes = verificacoes;
            this.detalhe      = detalhe;
        }
    }

    public ResultadoAnalise analisarBoleto(Boleto boletoRecebido, Boleto boletoDoBanco) {

        boolean valorOk      = boletoRecebido.getValor().compareTo(boletoDoBanco.getValor()) == 0;
        boolean vencimentoOk = boletoRecebido.getDataVencimento().equals(boletoDoBanco.getDataVencimento());
        boolean bancoOk      = boletoRecebido.getBancoEmissor() != null 
    && boletoRecebido.getBancoEmissor().equals(boletoDoBanco.getBancoEmissor());
        boolean cnpjOk       = boletoRecebido.getBeneficiario().getCnpj()
                                    .equals(boletoDoBanco.getBeneficiario().getCnpj());
        boolean razaoOk      = boletoRecebido.getBeneficiario() != null 
    && boletoRecebido.getBeneficiario().getRazaoSocial() != null 
    && boletoRecebido.getBeneficiario().getRazaoSocial().equalsIgnoreCase(boletoDoBanco.getBeneficiario().getRazaoSocial());
        
    List<Verificacao> verificacoes = new ArrayList<>();
    
    verificacoes.add(new Verificacao(
        "Valor confere com o banco", 
        valorOk, 
        boletoRecebido.getValor() != null ? "R$ " + boletoRecebido.getValor().toString() : "Não informado", 
        "R$ " + boletoDoBanco.getValor().toString()
    ));

    verificacoes.add(new Verificacao(
        "Data de vencimento confere", 
        vencimentoOk, 
        boletoRecebido.getDataVencimento() != null ? boletoRecebido.getDataVencimento().toString() : "Não informada", 
        boletoDoBanco.getDataVencimento().toString()
    ));

    verificacoes.add(new Verificacao(
        "Banco emissor confere", 
        bancoOk, 
        boletoRecebido.getBancoEmissor() != null ? boletoRecebido.getBancoEmissor() : "Não informado", 
        boletoDoBanco.getBancoEmissor()
    ));

    verificacoes.add(new Verificacao(
        "CNPJ do beneficiário válido", 
        cnpjOk, 
        (boletoRecebido.getBeneficiario() != null && boletoRecebido.getBeneficiario().getCnpj() != null) ? boletoRecebido.getBeneficiario().getCnpj() : "Não informado", 
        boletoDoBanco.getBeneficiario().getCnpj()
    ));

    verificacoes.add(new Verificacao(
        "Razão social confere", 
        razaoOk, 
        (boletoRecebido.getBeneficiario() != null && boletoRecebido.getBeneficiario().getRazaoSocial() != null) ? boletoRecebido.getBeneficiario().getRazaoSocial() : "Não informada", 
        boletoDoBanco.getBeneficiario().getRazaoSocial()
    ));

        boolean possuiInconsistencia = !valorOk || !vencimentoOk || !bancoOk || !cnpjOk || !razaoOk;
        String statusBoleto       = possuiInconsistencia ? "suspeito" : "seguro";
        String statusBeneficiario = beneficiarioService.analisarBeneficiario(boletoDoBanco.getBeneficiario());

        StringBuilder inconsistencias = new StringBuilder();
        if (!valorOk)      inconsistencias.append("valor divergente; ");
        if (!vencimentoOk) inconsistencias.append("vencimento divergente; ");
        if (!bancoOk)      inconsistencias.append("banco divergente; ");
        if (!cnpjOk)       inconsistencias.append("CNPJ divergente; ");
        if (!razaoOk)      inconsistencias.append("razão social divergente; ");
        String detalhePrefixo = inconsistencias.length() > 0
            ? inconsistencias.toString()
            : "nenhuma inconsistência detectada; ";

        // Análise ML
        MlResultado ml = mlFraudService.analisar(boletoRecebido, boletoDoBanco);

        if (ml.isDisponivel()) {
            if (ml.scoreFraude >= 0.75) {
                return new ResultadoAnalise("Fraude", "ML", ml.scoreRisco, verificacoes,
                    detalhePrefixo + "ML classificou como fraude com alta confiança.");
            }
            if (ml.scoreSuspeito >= 0.50) {
                return new ResultadoAnalise("Suspeito", "ML", ml.scoreRisco, verificacoes,
                    detalhePrefixo + "ML classificou como suspeito.");
            }
            if (ml.scoreSeguro >= 0.70) {
                return new ResultadoAnalise("Seguro", "ML", ml.scoreRisco, verificacoes,
                    detalhePrefixo + "ML classificou como seguro.");
            }
        }

        // Fallback: regras fixas
        String statusFinal;
        if ("suspeito".equalsIgnoreCase(statusBoleto) && "suspeito".equalsIgnoreCase(statusBeneficiario)) {
            statusFinal = "Fraude";
        } else if ("fraude".equalsIgnoreCase(statusBeneficiario)) {
            statusFinal = "Fraude";
        } else if ("suspeito".equalsIgnoreCase(statusBoleto) || "suspeito".equalsIgnoreCase(statusBeneficiario)) {
            statusFinal = "Suspeito";
        } else {
            statusFinal = "Seguro";
        }

        return new ResultadoAnalise(statusFinal, "Regras fixas", null, verificacoes,
            detalhePrefixo + "ML indisponível ou indeciso — decisão pelas regras fixas.");
    }
}