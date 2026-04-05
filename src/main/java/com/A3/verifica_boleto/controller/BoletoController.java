package com.A3.verifica_boleto.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.A3.verifica_boleto.model.Boleto;
import com.A3.verifica_boleto.repository.BoletoRepository;


@RestController
@RequestMapping("/boletos")


public class BoletoController {
    private final BoletoRepository boletoRepository;

    public BoletoController(BoletoRepository boletoRepository) {
        this.boletoRepository = boletoRepository;
    }

    @GetMapping("/{linhaDigitavel}")
    public Boleto buscarPorLinhaDigitavel(@PathVariable String linhaDigitavel) {
        return boletoRepository.findByLinhaDigitavel(linhaDigitavel);
    }

}
