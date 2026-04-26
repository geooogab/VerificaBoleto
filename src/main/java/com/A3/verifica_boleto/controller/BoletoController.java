package com.A3.verifica_boleto.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.A3.verifica_boleto.model.Boleto;
import com.A3.verifica_boleto.repository.BoletoRepository;
import com.A3.verifica_boleto.service.BoletoService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/boletos")
public class BoletoController {
    private final BoletoRepository boletoRepository;
    private final BoletoService boletoService;

    @Autowired
    public BoletoController(BoletoRepository boletoRepository, BoletoService boletoService) {
        this.boletoRepository = boletoRepository;
        this.boletoService = boletoService;
    }

    // Listar todos os boletos
    @GetMapping
    public List<Boleto> listarTodos() {
        return boletoRepository.findAll();
    }

    // Buscar boleto por linha digitável
    @GetMapping("/{linhaDigitavel}")
    public Boleto buscarPorLinhaDigitavel(@PathVariable String linhaDigitavel) {
        return boletoRepository.findByLinhaDigitavel(linhaDigitavel).orElse(null);
    }

    // Novo endpoint: analisar boleto
    @PostMapping("/analise")
    public ResponseEntity<String> analisar(@RequestBody Boleto boletoRecebido) {
        Optional<Boleto> boletoOpt = boletoRepository.findByLinhaDigitavel(boletoRecebido.getLinhaDigitavel());

        if (boletoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Boleto não encontrado no banco de dados");
        }

        Boleto boletoDoBanco = boletoOpt.get();
        String resultado = boletoService.analisarBoleto(boletoRecebido, boletoDoBanco);

        return ResponseEntity.ok(resultado);
    }
}

    




