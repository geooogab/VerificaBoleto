package com.A3.verifica_boleto.service;

//imports para extracao e conversao do texto, entendimento do PDF pelo java
//ferramentas nativas do java para localizar arquivos no disco e gerenciar erros de leitura
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.A3.verifica_boleto.model.Beneficiario;
import com.A3.verifica_boleto.model.Boleto;

import org.springframework.stereotype.Service;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;


@Service
public class PdfService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
        
    public String extrairTexto(File arquivo){ //File arquivo é o parãmetro de entrada, o metodo precisa que seja entregue um arquivo para que ele tenha o que ler
        try (PDDocument document = Loader.loadPDF(arquivo)) { // O try-with resources fechas o arquivo sozinho
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // ajuda a manter a ordem visual das palavras
            return stripper.getText(document);
            
        } catch (IOException e){
        
            return null; // se der erro e não haver leitura o método retona "nada"
        }

    }

    public Boleto converterPdfParaObjeto(File arquivo){
        String texto = extrairTexto(arquivo);
        if (texto == null) return null;

        Boleto boleto = new Boleto();

        // Extrair o VALOR e converter para BigDecimal
       String valorStr = extrairPadrao(texto, "(?:R\\$\\s?)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2})");
       boleto.setValor(limparValor(valorStr));

       
        // extrair e converter datas para LOcalDate
        String dataVencStr = extrairPadrao(texto, "\\d{2}/\\d{2}/\\d{4}");
        try {
            if (!dataVencStr.equals("Não encontrado")) {
                boleto.setDataVencimento(LocalDate.parse(dataVencStr, formatter));
            }
        } catch (Exception e) {
        }
        
        // linha digitavel 
        String regexLinha = "(\\d{5}[.\\s]?\\d{5}[.\\s]?\\d{5}[.\\s]?\\d{6}[.\\s]?\\d{5}[.\\s]?\\d{6}[.\\s]?\\d[.\\s]?\\d{10,15})";
        String linhaDigitavel = extrairPadrao(texto, regexLinha);
        boleto.setLinhaDigitavel(linhaDigitavel.replaceAll("[\\s.]", ""));

        if (boleto.getLinhaDigitavel().equals("Não encontrado")){
            String regexLinha2 = "(\\d{11}-\\d[\\s\\n]*){4}";
            linhaDigitavel = extrairPadrao(texto, regexLinha2);
            boleto.setLinhaDigitavel(linhaDigitavel.replaceAll("[\\s.]", ""));
        }
        
        // CPF (000.000.000-00)
        String cpf = extrairPadrao(texto, "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
        boleto.setCpfPagador(cpf);


        String nome = extrairBeneficiario(texto);
        Beneficiario b = new Beneficiario();
        b.setRazaoSocial(nome);

        return boleto;
    }

    private String extrairBeneficiario(String textoCompleto) {
    // Esse regex ignora maiúsculas/minúsculas e procura o texto logo após
    // a palavra "Social" ou "Denominação" seguida de quebra de linha ou espaços
    Pattern pattern = Pattern.compile("(?i)(?:Social|Denominação Social)[:\\s\\n]+(.+)", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(textoCompleto);

    if (matcher.find()) {
        String nome = matcher.group(1).trim();
        return nome.split("\\s{2,}")[0]; 
    }
    return "Beneficiário não identificado";
    }


    private BigDecimal limparValor(String valorStr){
        if (valorStr.equals("Não encontrado")) return BigDecimal.ZERO;
        
        try{
            String limpo = valorStr.replaceAll("[R$\\s]", "").replace(".", "").replace(",", ".");
            return new BigDecimal(limpo);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String extrairPadrao(String texto, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(texto);

        if (matcher.find()){
            return matcher.group(0);
        }
        return "Não encontrado";
    }
    
    
}

