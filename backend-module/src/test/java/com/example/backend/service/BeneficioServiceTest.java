package com.example.backend.service;

import com.example.backend.dto.BeneficioRequestDTO;
import com.example.backend.dto.BeneficioResponseDTO;
import com.example.backend.dto.TransferRequestDTO;
import com.example.backend.entity.Beneficio;
import com.example.backend.exception.InsufficientBalanceException;
import com.example.backend.repository.BeneficioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficioServiceTest {

    @Mock
    private BeneficioRepository repository;

    @InjectMocks
    private BeneficioService service;

    private Beneficio beneficioA;
    private Beneficio beneficioB;

    @BeforeEach
    void setUp() {
        beneficioA = new Beneficio();
        beneficioA.setId(1L);
        beneficioA.setNome("Beneficio A");
        beneficioA.setValor(new BigDecimal("1000.00"));
        beneficioA.setAtivo(true);
        beneficioA.setVersion(0L);

        beneficioB = new Beneficio();
        beneficioB.setId(2L);
        beneficioB.setNome("Beneficio B");
        beneficioB.setValor(new BigDecimal("500.00"));
        beneficioB.setAtivo(true);
        beneficioB.setVersion(0L);
    }

    @Test
    @DisplayName("findAll deve retornar lista de todos os benefícios")
    void findAll_returnsAllBeneficios() {
        when(repository.findAll()).thenReturn(List.of(beneficioA, beneficioB));

        List<BeneficioResponseDTO> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNome()).isEqualTo("Beneficio A");
        assertThat(result.get(1).getNome()).isEqualTo("Beneficio B");
    }

    @Test
    @DisplayName("findById deve lançar EntityNotFoundException quando id não existe")
    void findById_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create deve persistir e retornar o benefício criado")
    void create_persistsAndReturnsDto() {
        BeneficioRequestDTO dto = new BeneficioRequestDTO();
        dto.setNome("Novo");
        dto.setValor(new BigDecimal("200.00"));
        dto.setAtivo(true);

        Beneficio saved = new Beneficio();
        saved.setId(3L);
        saved.setNome("Novo");
        saved.setValor(new BigDecimal("200.00"));
        saved.setAtivo(true);
        saved.setVersion(0L);

        when(repository.save(any(Beneficio.class))).thenReturn(saved);

        BeneficioResponseDTO result = service.create(dto);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getNome()).isEqualTo("Novo");
        assertThat(result.getValor()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("transfer deve debitar e creditar corretamente")
    void transfer_happyPath_updatesBalances() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(beneficioA));
        when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(beneficioB));
        when(repository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setAmount(new BigDecimal("300.00"));

        service.transfer(dto);

        assertThat(beneficioA.getValor()).isEqualByComparingTo("700.00");
        assertThat(beneficioB.getValor()).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("transfer deve lançar InsufficientBalanceException quando saldo insuficiente")
    void transfer_throwsWhenInsufficientBalance() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(beneficioA));
        when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(beneficioB));

        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setAmount(new BigDecimal("9999.00"));

        assertThatThrownBy(() -> service.transfer(dto))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    @DisplayName("transfer deve lançar IllegalArgumentException quando fromId == toId")
    void transfer_throwsWhenSameIds() {
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setFromId(1L);
        dto.setToId(1L);
        dto.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.transfer(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origem e destino não podem ser iguais");
    }

    @Test
    @DisplayName("delete deve chamar repository.delete quando id existe")
    void delete_callsRepositoryDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(beneficioA));

        service.delete(1L);

        verify(repository, times(1)).delete(beneficioA);
    }
}
