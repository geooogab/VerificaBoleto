package com.A3.verifica_boleto.service;

import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ChatbotService {

    private final Client client;

    public ChatbotService(@Value("${gemini.api.key}") String apiKey) {
        
        this.client = Client.builder().apiKey(apiKey).build();}

    public String responderPergunta(String pergunta) {

        try {
        String prompt = "Você é um assistente especializado em boletos e segurança financeira. "
                      + "Responda de forma clara, breve e eficiente: " + pergunta;

        GenerateContentResponse response =
            client.models.generateContent("gemini-2.5-flash", prompt, null);

        return response.text();
        } catch (Exception e) {
        return "Desculpe, não consegui processar sua pergunta no momento. Tente novamente.";
    
    }
  }
}
