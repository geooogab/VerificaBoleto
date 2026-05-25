package com.A3.verifica_boleto.util;

import com.A3.verifica_boleto.model.Boleto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class LinhaDigitavelParser {

    // Lista interna de bancos
    private static final Map<String, String> BANCOS = new HashMap<>();
    static {
        BANCOS.put("001", "Banco do Brasil");
        BANCOS.put("033", "Santander");
        BANCOS.put("104", "Caixa Econômica");
        BANCOS.put("237", "Bradesco");
        BANCOS.put("341", "Itaú");
    }

    public static void preencherDados(Boleto boleto) {
        String linhaDigitavel = boleto.getLinhaDigitavel();

        // Remove espaços e pontos
        String apenasNumeros = linhaDigitavel.replaceAll("\\D", "");

        if (apenasNumeros.length() != 47) {
            throw new IllegalArgumentException("Linha digitável inválida");
        }

        // Banco emissor
        String bancoCodigo = apenasNumeros.substring(0, 3);
        String bancoNome = BANCOS.getOrDefault(bancoCodigo, "Banco desconhecido");
        boleto.setBancoEmissor(bancoNome);

        // Valor (últimos 10 dígitos, em centavos)
        String valorStr = apenasNumeros.substring(37, 47);
        BigDecimal valor = new BigDecimal(valorStr).movePointLeft(2);
        boleto.setValor(valor);
    }
}





