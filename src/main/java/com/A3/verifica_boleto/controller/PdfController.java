package com.A3.verifica_boleto.controller;

import com.A3.verifica_boleto.model.Boleto;
import com.A3.verifica_boleto.service.PdfService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pdf")
@Tag(name = "PDF", description = "Extração de dados de boletos via upload de PDF")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Operation(
        summary = "Extrair dados do PDF",
        description = "Recebe um arquivo PDF de boleto e extrai os dados (valor, vencimento, linha digitável, beneficiário). " +
                      "O resultado é usado para preencher os campos do formulário no frontend, permitindo que o usuário confira as informações antes de enviar para análise via POST /boletos/analise."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dados extraídos com sucesso",
            content = @Content(schema = @Schema(implementation = Boleto.class))),
        @ApiResponse(responseCode = "400", description = "Não foi possível extrair os dados do PDF", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro interno ao processar o arquivo", content = @Content)
    })
    @PostMapping(value = "/extrair", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extrair(@RequestParam("arquivo") MultipartFile arquivo) {
        try {
            Boleto boleto = pdfService.converterPdfParaObjeto(arquivo.getBytes());
            if (boleto == null) {
                return ResponseEntity.badRequest().body("Não foi possível extrair os dados do PDF.");
            }
            return ResponseEntity.ok(boleto);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao ler o arquivo PDF: " + e.getMessage());
        }
    }
}