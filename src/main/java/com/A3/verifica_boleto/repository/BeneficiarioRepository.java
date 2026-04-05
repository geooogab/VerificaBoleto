package com.A3.verifica_boleto.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.A3.verifica_boleto.model.*;


public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long>{
    Beneficiario findByCnpj(String cnpj);


}
