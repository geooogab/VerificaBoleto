package com.A3.verifica_boleto.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.A3.verifica_boleto.model.Boleto;
import com.A3.verifica_boleto.repository.BoletoRepository;
import com.A3.verifica_boleto.service.BoletoService;
import com.A3.verifica_boleto.service.BoletoService.ResultadoAnalise;
import com.A3.verifica_boleto.util.LinhaDigitavelParser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/boletos")
@Tag(name = "Boletos", description = "Endpoints para análise e consulta de boletos bancários")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BoletoController {

    private final BoletoRepository boletoRepository;
    private final BoletoService boletoService;

    @Autowired
    public BoletoController(BoletoRepository boletoRepository, BoletoService boletoService) {
        this.boletoRepository = boletoRepository;
        this.boletoService    = boletoService;
    }

    @Operation(summary = "Listar todos os boletos", description = "Retorna todos os boletos cadastrados no banco de dados.")
    @ApiResponse(responseCode = "200", description = "Lista de boletos retornada com sucesso")
    @GetMapping
    public List<Boleto> listarTodos() {
        return boletoRepository.findAll();
    }

    @Operation(summary = "Buscar boleto por linha digitável", description = "Retorna os dados de um boleto específico a partir da sua linha digitável.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boleto encontrado"),
        @ApiResponse(responseCode = "204", description = "Boleto não encontrado", content = @Content)
    })
    @GetMapping("/{linhaDigitavel}")
    public Boleto buscarPorLinhaDigitavel(
            @Parameter(description = "Linha digitável do boleto (47 dígitos sem espaços ou pontos)")
            @PathVariable String linhaDigitavel) {
        return boletoRepository.findByLinhaDigitavel(linhaDigitavel).orElse(null);
    }

    @Operation(
        summary = "Decodificar linha digitável",
        description = "Extrai e preenche os dados do boleto (banco emissor e valor) a partir da linha digitável informada."
    )
    @ApiResponse(responseCode = "200", description = "Dados preenchidos com sucesso")
    @PostMapping("/parse")
    public ResponseEntity<Boleto> parseLinhaDigitavel(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Boleto com a linha digitável preenchida")
            @RequestBody Boleto boletoRecebido) {
        LinhaDigitavelParser.preencherDados(boletoRecebido);
        return ResponseEntity.ok(boletoRecebido);
    }

    @Operation(
        summary = "Analisar boleto (input manual)",
        description = "Recebe os dados do boleto informados manualmente pelo usuário, compara com o boleto oficial no banco de dados e retorna o resultado da análise híbrida (regras fixas + ML)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Análise realizada com sucesso",
            content = @Content(schema = @Schema(implementation = ResultadoAnalise.class))),
        @ApiResponse(responseCode = "400", description = "Boleto não encontrado no banco de dados", content = @Content)
    })
    @PostMapping("/analise")
    public ResponseEntity<ResultadoAnalise> analisar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do boleto recebido pelo usuário")
            @RequestBody Boleto boletoRecebido) {
        Optional<Boleto> boletoOpt = boletoRepository.findByLinhaDigitavel(boletoRecebido.getLinhaDigitavel());

        if (boletoOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Boleto boletoDoBanco = boletoOpt.get();
        ResultadoAnalise resultado = boletoService.analisarBoleto(boletoRecebido, boletoDoBanco);
        return ResponseEntity.ok(resultado);
    }
}