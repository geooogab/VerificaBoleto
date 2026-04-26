package com.A3.verifica_boleto.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import com.A3.verifica_boleto.model.Beneficiario;

@Service
public class BeneficiarioService {

    public String analisarBeneficiario(Beneficiario beneficiario) {
        
        
        // 1. Data de abertura (menos de 90 dias → suspeito)
        long diasDesdeAbertura = ChronoUnit.DAYS.between(beneficiario.getDataAbertura(), LocalDate.now());
        if (diasDesdeAbertura < 90) {
            return "suspeito";
        }

        // 2. Reputação (menor que 50 → suspeito)
        if (beneficiario.getReputacaoScore() < 50) {
            return "suspeito";
        }

        // 3. Histórico de fraude (> 0 → bloqueado)
        if (beneficiario.getHistoricoFraude() > 0) {
            return "fraude";
        }
        // Se passou em todas as regras → confiável
        return "confiavel";
    }
}



