package com.example.backend;

import com.example.backend.dto.BeneficioRequestDTO;
import com.example.backend.dto.TransferRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BeneficioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/beneficios deve retornar 200 com 2 registros seed")
    void listAll_returnsSeedData() throws Exception {
        mockMvc.perform(get("/api/v1/beneficios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Beneficio A"))
                .andExpect(jsonPath("$[1].nome").value("Beneficio B"));
    }

    @Test
    @DisplayName("POST /api/v1/beneficios deve retornar 201 com Location header")
    void create_returns201WithLocation() throws Exception {
        BeneficioRequestDTO dto = new BeneficioRequestDTO();
        dto.setNome("Novo Beneficio");
        dto.setValor(new BigDecimal("250.00"));
        dto.setAtivo(true);

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Novo Beneficio"))
                .andExpect(jsonPath("$.valor").value(250.00));
    }

    @Test
    @DisplayName("POST /api/v1/beneficios com nome em branco deve retornar 400")
    void create_withBlankNome_returns400() throws Exception {
        BeneficioRequestDTO dto = new BeneficioRequestDTO();
        dto.setNome("");
        dto.setValor(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/beneficios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nome").exists());
    }

    @Test
    @DisplayName("GET /api/v1/beneficios/{id} inexistente deve retornar 404")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/beneficios/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("9999")));
    }

    @Test
    @DisplayName("POST /api/v1/beneficios/transfer deve transferir e retornar 200")
    void transfer_happyPath_returns200() throws Exception {
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setAmount(new BigDecimal("200.00"));

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/beneficios/1"))
                .andExpect(jsonPath("$.valor").value(800.00));

        mockMvc.perform(get("/api/v1/beneficios/2"))
                .andExpect(jsonPath("$.valor").value(700.00));
    }

    @Test
    @DisplayName("POST /api/v1/beneficios/transfer com saldo insuficiente deve retornar 422")
    void transfer_insufficientBalance_returns422() throws Exception {
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setFromId(2L);
        dto.setToId(1L);
        dto.setAmount(new BigDecimal("9999.00"));

        mockMvc.perform(post("/api/v1/beneficios/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("Saldo insuficiente")));
    }

    @Test
    @DisplayName("DELETE /api/v1/beneficios/{id} deve retornar 204 e GET subsequente 404")
    void delete_returns204_thenGetReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/beneficios/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/beneficios/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/beneficios/{id} deve atualizar e retornar 200")
    void update_returnsUpdatedBeneficio() throws Exception {
        BeneficioRequestDTO dto = new BeneficioRequestDTO();
        dto.setNome("Beneficio A Atualizado");
        dto.setValor(new BigDecimal("1500.00"));
        dto.setAtivo(true);

        mockMvc.perform(put("/api/v1/beneficios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Beneficio A Atualizado"))
                .andExpect(jsonPath("$.valor").value(1500.00));
    }
}
