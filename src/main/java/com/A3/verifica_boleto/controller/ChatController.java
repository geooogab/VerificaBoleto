package com.A3.verifica_boleto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.A3.verifica_boleto.service.ChatbotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chatbot", description = "Assistente virtual especializado em boletos e segurança financeira")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {

    private final ChatbotService chatbotService;

    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @Operation(
        summary = "Enviar pergunta ao assistente",
        description = "Envia uma pergunta em texto livre ao assistente virtual baseado no Gemini. O assistente é especializado em boletos bancários, fraudes e segurança financeira, e responde dúvidas sobre o sistema e resultados de análise."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso",
            content = @Content(schema = @Schema(type = "string", example = "Um boleto falso geralmente apresenta..."))),
        @ApiResponse(responseCode = "500", description = "Erro ao processar a pergunta", content = @Content)
    })
    
    @PostMapping
    public ResponseEntity<String> conversar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Pergunta em texto livre",
                content = @Content(schema = @Schema(type = "string", example = "O que significa score de risco 85?"))
            )
            @RequestBody String pergunta) {
        String resposta = chatbotService.responderPergunta(pergunta);
        return ResponseEntity.ok(resposta);
    }
}