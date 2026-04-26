package com.A3.verifica_boleto.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.A3.verifica_boleto.model.*;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long>{
    Optional <Beneficiario> findByCnpj(String cnpj);


}
