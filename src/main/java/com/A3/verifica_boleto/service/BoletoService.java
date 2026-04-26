package com.A3.verifica_boleto.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.A3.verifica_boleto.model.Boleto;

@Service
public class BoletoService {
    @Autowired
private BeneficiarioService beneficiarioService;


    public String analisarBoleto(Boleto boletoRecebido, Boleto boletoDoBanco) {
        String statusBoleto = "seguro";

        //  Verificar inconsistências
        if (boletoRecebido.getValor() != boletoDoBanco.getValor() ||
            !boletoRecebido.getDataVencimento().equals(boletoDoBanco.getDataVencimento()) ||
            !boletoRecebido.getBancoEmissor().equals(boletoDoBanco.getBancoEmissor())) {

            statusBoleto = "suspeito";
        }
         

        //  Verificar inconsistências beneficiário (CNPJ e razão social)
        if (!boletoRecebido.getBeneficiario().getCnpj().equals(boletoDoBanco.getBeneficiario().getCnpj()) ||
            !boletoRecebido.getBeneficiario().getRazaoSocial().equalsIgnoreCase(boletoDoBanco.getBeneficiario().getRazaoSocial())) {

            statusBoleto = "suspeito";
        }

        //  Verificar beneficiário
        String statusBeneficiario = beneficiarioService.analisarBeneficiario(boletoDoBanco.getBeneficiario());


        //  Análise final 
        if ("suspeito".equalsIgnoreCase(statusBoleto) && 
            "suspeito".equalsIgnoreCase(statusBeneficiario)) {
            return "fraude";
        }

        if ("fraude".equalsIgnoreCase(statusBeneficiario)) {
            return "fraude";
        }

        if ("suspeito".equalsIgnoreCase(statusBoleto) || 
            "suspeito".equalsIgnoreCase(statusBeneficiario)) {
            return "suspeito";
        }

        return "seguro";
    }
}
