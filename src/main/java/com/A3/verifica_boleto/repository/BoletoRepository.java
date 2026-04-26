package com.A3.verifica_boleto.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.A3.verifica_boleto.model.Boleto;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    Optional <Boleto> findByLinhaDigitavel(String linhaDigitavel);
}



