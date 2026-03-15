package com.example.backend.service;

import com.example.backend.dto.BeneficioRequestDTO;
import com.example.backend.dto.BeneficioResponseDTO;
import com.example.backend.dto.TransferRequestDTO;
import com.example.backend.entity.Beneficio;
import com.example.backend.exception.InsufficientBalanceException;
import com.example.backend.repository.BeneficioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BeneficioService {

    private final BeneficioRepository repository;

    public BeneficioService(BeneficioRepository repository) {
        this.repository = repository;
    }

    public List<BeneficioResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BeneficioResponseDTO findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public BeneficioResponseDTO create(BeneficioRequestDTO dto) {
        Beneficio entity = new Beneficio();
        applyDto(entity, dto);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public BeneficioResponseDTO update(Long id, BeneficioRequestDTO dto) {
        Beneficio entity = getOrThrow(id);
        applyDto(entity, dto);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Beneficio entity = getOrThrow(id);
        repository.delete(entity);
    }

    @Transactional
    public void transfer(TransferRequestDTO dto) {
        if (dto.getFromId().equals(dto.getToId())) {
            throw new IllegalArgumentException("Origem e destino não podem ser iguais");
        }

        Long firstId  = dto.getFromId() < dto.getToId() ? dto.getFromId() : dto.getToId();
        Long secondId = dto.getFromId() < dto.getToId() ? dto.getToId()   : dto.getFromId();

        Beneficio first  = repository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new EntityNotFoundException("Benefício " + firstId + " não encontrado"));
        Beneficio second = repository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new EntityNotFoundException("Benefício " + secondId + " não encontrado"));

        Beneficio from = dto.getFromId().equals(firstId) ? first : second;
        Beneficio to   = dto.getToId().equals(firstId)   ? first : second;

        if (from.getValor().compareTo(dto.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                "Saldo insuficiente: disponível " + from.getValor() + ", solicitado " + dto.getAmount());
        }

        from.setValor(from.getValor().subtract(dto.getAmount()));
        to.setValor(to.getValor().add(dto.getAmount()));

        repository.save(from);
        repository.save(to);
    }

    private Beneficio getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Benefício com id " + id + " não encontrado"));
    }

    private void applyDto(Beneficio entity, BeneficioRequestDTO dto) {
        entity.setNome(dto.getNome());
        entity.setDescricao(dto.getDescricao());
        entity.setValor(dto.getValor());
        entity.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : Boolean.TRUE);
    }

    private BeneficioResponseDTO toResponse(Beneficio e) {
        return new BeneficioResponseDTO(
                e.getId(), e.getNome(), e.getDescricao(),
                e.getValor(), e.getAtivo(), e.getVersion());
    }
}
