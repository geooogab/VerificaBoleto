package com.A3.verifica_boleto.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import com.A3.verifica_boleto.model.Beneficiario;
import com.A3.verifica_boleto.model.Boleto;
import com.A3.verifica_boleto.util.LinhaDigitavelParser;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ALTERADO: recebe byte[] em vez de File para evitar problema de arquivo corrompido no Windows
    public String extrairTexto(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) { 
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); 
            return stripper.getText(document);
        } catch (IOException e) {
            return null; 
        }
    }

    // ALTERADO: recebe byte[] em vez de File
    public Boleto converterPdfParaObjeto(byte[] bytes) {
        String texto = extrairTexto(bytes);
        if (texto == null) return null;

        // LOG TEMPORÁRIO
    System.out.println("===== TEXTO BRUTO =====");
    System.out.println(texto);
    System.out.println("=======================");

        Boleto boleto = new Boleto();

        // Extrair o valor e converter para BigDecimal
        String valorStr = extrairPadrao(texto, "(?:R\\$\\s*)?(\\d+(?:\\.\\d{3})*,\\d{2})");
        boleto.setValor(valorStr != null ? limparValor(valorStr) : BigDecimal.ZERO);

        // Extrair e converter datas para LocalDate
        String dataVencStr = extrairPadrao(texto, "(?i)Vencimento[^0-9]*(\\d{2}/\\d{2}/\\d{4})");
        try {
            if (dataVencStr != null) {
                boleto.setDataVencimento(LocalDate.parse(dataVencStr, formatter));
            }
        } catch (Exception e) {

}

        // ALTERADO: linha digitável com pontos e espaços — limpa depois
        String linhaDigitavel = extrairPadrao(texto,
            "(\\d{5}\\.\\d{5}\\s+\\d{5}\\.\\d{6}\\s+\\d{5}\\.\\d{6}\\s+\\d\\s+\\d{14})");
        if (linhaDigitavel != null) {
            boleto.setLinhaDigitavel(linhaDigitavel.replaceAll("[\\s.]", ""));
        }

        // CPF (000.000.000-00)
        // ALTERADO: adicionado contexto "CPF" antes do padrão para não pegar CNPJ por engano
        String cpf = extrairPadrao(texto, "CPF[:\\s]*(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})");
        boleto.setCpfPagador(cpf);

        
        // ALTERADO: adicionado contexto "CNPJ" antes do padrão para pegar o CNPJ correto

        String cnpj = extrairPadrao(texto, "(?i)CNPJ[^\\d\\r\\n]*[\\r\\n\\s]*(\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})");

        String nome = null;
        
        if (cnpj != null) {
            int indexCnpj = texto.indexOf(cnpj);
            if (indexCnpj != -1) {
                
                String textoAnterior = texto.substring(0, indexCnpj).trim();
                String[] linhas = textoAnterior.split("\\r?\\n");
                
                
                for (int i = linhas.length - 1; i >= 0; i--) {
    String linhaLimpa = linhas[i].trim();

    if (linhaLimpa.isEmpty()) continue;

    // Aceita só linhas que parecem razão social:
    // tem pelo menos 5 caracteres, começa com letra, e não é só números/símbolos
    boolean pareceNome = linhaLimpa.length() >= 5
        && Character.isLetter(linhaLimpa.charAt(0))
        && linhaLimpa.matches(".*[a-zA-ZÀ-ú]{3,}.*");

    // Rejeita rótulos estruturais conhecidos
    boolean ehRotulo = linhaLimpa.toLowerCase().matches(
        ".*(cnpj|endereço|agência|banco|benefici|cedente|ficha|compensação|pagador|vencimento|documento|instrução|sacador|autenticação|recibo).*"
    );

    if (pareceNome && !ehRotulo) {
        nome = linhaLimpa;
        break;
    }
}
                }
            }
        

        
        if (nome == null) {
            nome = extrairPadrao(texto, "(?i)(?:Benefici[aá]rio|Cedente)[:\\s]*\\r?\\n?([A-Za-z0-9][^\\n\\r]+)");
        }

        Beneficiario b = new Beneficiario();
        b.setRazaoSocial(nome != null ? nome.trim() : null);
        b.setCnpj(cnpj != null ? cnpj.replaceAll("[./-]", "") : null);
        boleto.setBeneficiario(b);


        // ADICIONADO: preenche bancoEmissor a partir da linha digitável
        if (boleto.getLinhaDigitavel() != null) {
    
        BigDecimal valorExtraido = boleto.getValor();
        LinhaDigitavelParser.preencherDados(boleto);
        boleto.setValor(valorExtraido);
    }

        return boleto;
    }

    private BigDecimal limparValor(String valorStr) {
        try {
            String limpo = valorStr.replaceAll("[R$\\s]", "").replace(".", "").replace(",", ".");
            return new BigDecimal(limpo);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String extrairPadrao(String texto, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? matcher.group(1) : null;
    }
}