# Documentação Técnica — BIP Teste Integrado

## Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [API — Endpoints](#api--endpoints)
- [Swagger / OpenAPI](#swagger--openapi)
- [Bug Corrigido no EJB](#bug-corrigido-no-ejb)
- [Testes](#testes)
- [CI/CD](#cicd)
- [Estrutura de Pastas](#estrutura-de-pastas)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)

---

## Visão Geral

Aplicação de gerenciamento de **Benefícios** com CRUD completo e transferência de valores entre benefícios. Demonstra arquitetura em camadas com controle transacional, locking para concorrência segura e interface em Angular Material.

---

## Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                   Frontend Angular                   │
│          (Angular 17 + Angular Material)             │
└────────────────────┬────────────────────────────────┘
                     │ HTTP REST (JSON)
                     ▼
┌─────────────────────────────────────────────────────┐
│              Backend Spring Boot 3.2.5               │
│   Controllers → Services → Repositories → Database  │
└────────────────────┬────────────────────────────────┘
                     │ JPA / H2 (PostgreSQL mode)
                     ▼
┌─────────────────────────────────────────────────────┐
│                  Banco H2 (em memória)               │
│           schema.sql  +  seed.sql                    │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│               EJB Module (Jakarta EE)                │
│   BeneficioEjbService — lógica transacional EJB      │
└─────────────────────────────────────────────────────┘
```

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|------------|--------------|
| Java (JDK) | 17           |
| Maven      | 3.8+         |
| Node.js    | 20+          |
| npm        | 9+           |
| Git        | qualquer     |

---

## Como Executar

### 1. Banco de Dados

O backend usa **H2 em memória** no modo PostgreSQL. Os scripts `db/schema.sql` e `db/seed.sql` são executados automaticamente ao subir o Spring Boot.

Para inspecionar manualmente:
- H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:beneficiodb`
- Usuário: `sa` | Senha: *(em branco)*

### 2. EJB Module

```bash
cd ejb-module
mvn clean package
```

### 3. Backend Spring Boot

```bash
cd backend-module
mvn clean spring-boot:run
```

Disponível em: `http://localhost:8080`

### 4. Frontend Angular

```bash
cd frontend
npm install
npm start
```

Disponível em: `http://localhost:4200`

> O frontend aponta para `http://localhost:8080/api/v1` (configurado em `environment.ts`).

---

## API — Endpoints

Base URL: `http://localhost:8080/api/v1`

### Benefícios

| Método | Endpoint              | Descrição          | Status          |
|--------|-----------------------|--------------------|-----------------|
| GET    | `/beneficios`         | Listar todos       | 200             |
| GET    | `/beneficios/{id}`    | Buscar por ID      | 200, 404        |
| POST   | `/beneficios`         | Criar              | 201, 400        |
| PUT    | `/beneficios/{id}`    | Atualizar          | 200, 400, 404, 409 |
| DELETE | `/beneficios/{id}`    | Remover            | 204, 404        |
| POST   | `/beneficios/transfer`| Transferir valor   | 200, 400, 422   |

### Corpo de Request — Criar/Atualizar

```json
{
  "nome": "Beneficio C",
  "descricao": "Descrição opcional",
  "valor": 750.00,
  "ativo": true
}
```

### Corpo de Request — Transferência

```json
{
  "fromId": 1,
  "toId": 2,
  "amount": 200.00
}
```

### Formato de Erro (todos os endpoints)

```json
{
  "timestamp": "2026-03-15T10:00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente para transferência"
}
```

| Código | Situação |
|--------|----------|
| 400 | Dados inválidos (campo obrigatório ausente, valor negativo, etc.) |
| 404 | Benefício não encontrado |
| 409 | Conflito de versão (optimistic lock) |
| 422 | Saldo insuficiente para transferência |

---

## Swagger / OpenAPI

Com o backend rodando, acesse:

- **Interface visual:** `http://localhost:8080/swagger-ui.html`
- **Especificação JSON:** `http://localhost:8080/api-docs`

Todos os endpoints, schemas e códigos de retorno estão documentados interativamente.

---

## Bug Corrigido no EJB

### O problema

O método `transfer` do `BeneficioEjbService` original:
- Não verificava se havia saldo suficiente
- Não usava locking (bloqueio de linha no banco)
- Não fazia rollback em caso de erro
- Sujeito a race condition em acessos simultâneos

### A correção

**Arquivo:** `ejb-module/src/main/java/com/example/ejb/BeneficioEjbService.java`

1. **Validações obrigatórias** antes de qualquer operação:
   - Parâmetros não nulos
   - Valor > 0
   - `fromId != toId` (impede autotransferência)
   - Saldo de origem >= valor da transferência

2. **Pessimistic Locking** (`PESSIMISTIC_WRITE`):
   - Bloqueia as duas linhas no banco antes de alterar qualquer valor
   - Nenhuma outra transação lê ou escreve nesses registros até o commit

3. **Ordenação para evitar deadlock**:
   - Locks sempre adquiridos pelo menor ID primeiro
   - Garante que duas transações concorrentes não fiquem esperando uma pela outra indefinidamente

4. **Rollback automático**:
   - `InsufficientBalanceException` anotada com `@ApplicationException(rollback = true)`
   - Qualquer exceção desfaz a transação inteira

5. **Optimistic Locking** (campo `version`):
   - Detecta conflitos quando dois processos tentam alterar o mesmo registro ao mesmo tempo

```java
// Pseudocódigo da correção
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    validarParametros(fromId, toId, amount);

    // Adquire locks em ordem crescente de ID (evita deadlock)
    Long firstId  = min(fromId, toId);
    Long secondId = max(fromId, toId);
    Beneficio first  = em.find(Beneficio.class, firstId,  PESSIMISTIC_WRITE);
    Beneficio second = em.find(Beneficio.class, secondId, PESSIMISTIC_WRITE);

    Beneficio from = fromId.equals(firstId) ? first : second;
    Beneficio to   = toId.equals(firstId)   ? first : second;

    if (from.getValor().compareTo(amount) < 0) {
        throw new InsufficientBalanceException(); // rollback automático
    }

    from.setValor(from.getValor().subtract(amount));
    to.setValor(to.getValor().add(amount));
    // JPA persiste automaticamente ao commitar a transação
}
```

---

## Testes

### Unitários — BeneficioServiceTest

```bash
cd backend-module
mvn test
```

| # | Teste | O que valida |
|---|-------|-------------|
| 1 | `findAll_returnsAllBeneficios` | Lista retorna todos os registros |
| 2 | `findById_throwsWhenNotFound` | ID inexistente lança exceção 404 |
| 3 | `create_persistsAndReturnsDto` | Criar persiste e retorna DTO correto |
| 4 | `transfer_happyPath_updatesBalances` | Transferência válida atualiza saldos |
| 5 | `transfer_throwsWhenInsufficientBalance` | Saldo insuficiente lança exceção |
| 6 | `transfer_throwsWhenSameIds` | Autotransferência lança exceção |
| 7 | `delete_callsRepositoryDelete` | Delete aciona o repositório |

### Integração — BeneficioControllerIntegrationTest

```bash
cd backend-module
mvn verify
```

Testa o fluxo HTTP completo com banco H2 real e dados do seed:

| # | Cenário | Status esperado |
|---|---------|----------------|
| 1 | GET /beneficios retorna 2 registros do seed | 200 |
| 2 | POST cria e retorna header Location | 201 |
| 3 | POST com nome vazio | 400 |
| 4 | GET por ID inexistente | 404 |
| 5 | POST /transfer bem-sucedida atualiza saldos | 200 |
| 6 | POST /transfer saldo insuficiente | 422 |
| 7 | DELETE + GET retorna 404 após exclusão | 204 → 404 |
| 8 | PUT atualiza e retorna benefício atualizado | 200 |

### Frontend

```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless
```

---

## CI/CD

Pipeline em `.github/workflows/ci.yml` — executa em todo push e pull request.

| Job | Runtime | Comando principal |
|-----|---------|------------------|
| backend  | Ubuntu + JDK 17 | `mvn -B -f backend-module/pom.xml clean verify` |
| ejb      | Ubuntu + JDK 17 | `mvn -B -f ejb-module/pom.xml clean package` |
| frontend | Ubuntu + Node 20 | `npm install` → `build --prod` → `test --headless` |

Os três jobs rodam em paralelo e são independentes entre si.

---

## Estrutura de Pastas

```
bip-teste-integrado/
│
├── db/
│   ├── schema.sql                        # DDL da tabela BENEFICIO
│   └── seed.sql                          # 2 registros iniciais de teste
│
├── ejb-module/
│   └── src/main/java/com/example/ejb/
│       ├── BeneficioEjbService.java      # Serviço EJB — bug corrigido
│       ├── Beneficio.java                # Entidade JPA (version + PESSIMISTIC_WRITE)
│       └── InsufficientBalanceException.java
│
├── backend-module/
│   └── src/
│       ├── main/java/com/example/backend/
│       │   ├── BeneficioController.java      # REST endpoints + CORS + Swagger
│       │   ├── config/OpenApiConfig.java      # Configuração Swagger/OpenAPI
│       │   ├── dto/
│       │   │   ├── BeneficioRequestDTO.java   # Validações de entrada
│       │   │   ├── BeneficioResponseDTO.java  # Resposta da API
│       │   │   └── TransferRequestDTO.java    # Dados de transferência
│       │   ├── entity/Beneficio.java          # Entidade JPA com version
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java # Trata erros com HTTP correto
│       │   │   ├── EntityNotFoundException.java
│       │   │   └── InsufficientBalanceException.java
│       │   ├── repository/BeneficioRepository.java # findByIdForUpdate (lock)
│       │   └── service/BeneficioService.java       # Lógica de negócio
│       └── test/java/com/example/backend/
│           ├── service/BeneficioServiceTest.java             # 7 testes unitários
│           └── BeneficioControllerIntegrationTest.java       # 8 testes e2e
│
├── frontend/
│   └── src/app/
│       ├── app.component.ts              # Layout + navegação
│       ├── app.routes.ts                 # Rotas: / e /transfer
│       ├── app.config.ts                 # Providers + locale pt-BR
│       ├── core/
│       │   ├── models/beneficio.model.ts      # Interfaces TypeScript
│       │   └── services/beneficio.service.ts  # HttpClient para a API
│       └── features/beneficios/
│           ├── beneficio-list/           # Tabela com CRUD completo
│           ├── beneficio-form/           # Modal criar / editar
│           └── transfer-form/            # Tela de transferência
│
├── docs/
│   ├── README.md                         # Instruções originais do desafio
│   ├── DOCUMENTACAO.md                   # Este arquivo
│   └── EXPLICACAO_HUMANA.md              # Explicação em linguagem simples
│
└── .github/workflows/
    └── ci.yml                            # Pipeline CI — 3 jobs
```

---

## Tecnologias Utilizadas

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Banco | H2 (modo PostgreSQL) | 2.x |
| EJB | Jakarta EE / JPA | 3.x |
| Backend | Spring Boot | 3.2.5 |
| Backend | Java | 17 |
| Backend | Springdoc OpenAPI (Swagger) | 2.x |
| Backend | JUnit 5 + MockMvc | — |
| Frontend | Angular | 17.3.0 |
| Frontend | Angular Material | 17.x |
| Frontend | TypeScript | 5.x |
| Frontend | RxJS | 7.8 |
| CI | GitHub Actions | — |
| Build Backend | Maven | 3.8+ |
| Build Frontend | npm / Angular CLI | 20+ / 17+ |
