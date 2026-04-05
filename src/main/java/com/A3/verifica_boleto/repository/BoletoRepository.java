package com.A3.verifica_boleto.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.A3.verifica_boleto.model.Boleto;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    Boleto findByLinhaDigitavel(String linhaDigitavel);
}



