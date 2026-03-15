package com.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;

@Stateless
public class BeneficioEjbService {

    @PersistenceContext
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transfer(Long fromId, Long toId, BigDecimal amount)
            throws InsufficientBalanceException {

        if (fromId == null || toId == null || amount == null) {
            throw new IllegalArgumentException("fromId, toId e amount são obrigatórios");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser positivo");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Origem e destino não podem ser iguais");
        }

        // Adquire locks em ordem crescente de id para evitar deadlock
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        Beneficio first  = em.find(Beneficio.class, firstId,  LockModeType.PESSIMISTIC_WRITE);
        Beneficio second = em.find(Beneficio.class, secondId, LockModeType.PESSIMISTIC_WRITE);

        if (first == null || second == null) {
            throw new IllegalArgumentException("Benefício não encontrado");
        }

        Beneficio from = fromId.equals(firstId) ? first : second;
        Beneficio to   = toId.equals(firstId)   ? first : second;

        if (from.getValor().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                "Saldo insuficiente: disponível " + from.getValor() + ", solicitado " + amount);
        }

        from.setValor(from.getValor().subtract(amount));
        to.setValor(to.getValor().add(amount));
        // entidades managed — flush automático no commit, sem merge manual
    }
}
