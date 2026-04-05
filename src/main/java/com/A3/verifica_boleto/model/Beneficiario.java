package com.A3.verifica_boleto.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "beneficiario")


public class Beneficiario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 14, nullable = false, unique = true)
    private String cnpj;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "data_abertura")
    private LocalDate dataAbertura;

    @Column(name = "situacao", length = 50)
    private String situacao;

    @Column(name = "quantidade_boletos_mes")
    private Integer quantidadeBoletosMes;

    @Column(name = "valor_total_mes", precision = 12, scale = 2)
    private BigDecimal valorTotalMes;

    @Column(name = "ticket_medio", precision = 10, scale = 2)
    private BigDecimal ticketMedio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDate dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public Integer getQuantidadeBoletosMes() {
        return quantidadeBoletosMes;
    }

    public void setQuantidadeBoletosMes(Integer quantidadeBoletosMes) {
        this.quantidadeBoletosMes = quantidadeBoletosMes;
    }

    public BigDecimal getValorTotalMes() {
        return valorTotalMes;
    }

    public void setValorTotalMes(BigDecimal valorTotalMes) {
        this.valorTotalMes = valorTotalMes;
    }

    public BigDecimal getTicketMedio() {
        return ticketMedio;
    }

    public void setTicketMedio(BigDecimal ticketMedio) {
        this.ticketMedio = ticketMedio;
    }



}
