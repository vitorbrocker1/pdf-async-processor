# 📄 PDF Processing API

API REST em **Java + Spring Boot** que recebe arquivos PDF, processa de forma **assíncrona** (fila) e extrai o texto/dados do documento (OCR). Ideal para casos como leitura automática de documentos (RG, CPF, etc).

## 🧠 Como funciona (visão geral)

```
Cliente ──upload PDF──▶ API ──envia para──▶ RabbitMQ (fila) ──consome──▶ Worker ──OCR──▶ PostgreSQL
   ▲                                                                                        │
   └──────────────────────── consulta resultado (polling) ◀──────────────────────────────┘
```

1. Você **envia um PDF** para a API.
2. A API salva uma tarefa no banco com status `PENDING` e **publica na fila** (RabbitMQ).
3. Um **worker** (consumidor da fila) processa o PDF em segundo plano, faz o "OCR" e atualiza o status para `DONE` (sucesso) ou `FAILED` (erro).
4. Você **consulta o resultado** usando o ID da tarefa, quando quiser.

> Esse fluxo é assíncrono porque processar PDF pode demorar — então a API responde rápido (com um ID) e o processamento pesado acontece em segundo plano.

---

## 🛠️ Tecnologias usadas

| Tecnologia | Para quê serve |
|---|---|
| **Java 21** | Linguagem principal |
| **Spring Boot 3.3** | Framework principal da aplicação |
| **Spring Web** | Criação dos endpoints REST |
| **Spring Data JPA** | Acesso ao banco de dados |
| **PostgreSQL** | Banco de dados (armazena as tarefas e resultados) |
| **RabbitMQ** | Fila de mensagens (gerencia o processamento assíncrono) |
| **Docker / Docker Compose** | Roda tudo (app + banco + fila) sem precisar instalar nada manualmente |

---

## 📂 Estrutura do projeto

```
src/main/java/com/vitor/pdfapi/
├── PdfApiApplication.java       # Ponto de entrada da aplicação
├── config/
│   ├── AsyncConfig.java         # Configuração de processamento assíncrono
│   └── RabbitMQConfig.java      # Configuração das filas/exchanges do RabbitMQ
├── controller/
│   └── PdfController.java       # Endpoints da API (upload, resultado, status)
├── model/
│   ├── PdfJob.java               # Representa a tarefa salva no banco (status, resultado, etc)
│   └── PdfTask.java              # Representa a tarefa enviada para a fila
├── ocr/
│   ├── OcrService.java           # Interface do serviço de OCR
│   ├── MockOcrService.java       # Implementação simulada de OCR (usada fora de produção)
│   └── OcrException.java         # Exceção customizada de falhas no OCR
├── queue/
│   └── PdfQueueService.java      # Lógica de publicar/consultar a fila
├── repository/
│   └── PdfJobRepository.java     # Acesso ao banco (Spring Data JPA)
└── worker/
    └── PdfWorker.java            # "Escuta" a fila e processa os PDFs recebidos
```

---

## 🚀 Como rodar o projeto

### Opção 1 — Com Docker (recomendado, mais fácil)

Você só precisa ter o **Docker** e **Docker Compose** instalados.

```bash
docker-compose up --build
```

Isso vai automaticamente:
- Subir o **PostgreSQL** (porta `5432`)
- Subir o **RabbitMQ** (porta `5672`, painel web em `15672`)
- Buildar e subir a **API** (porta `8080`)

Quando terminar, a API estará disponível em:
```
http://localhost:8080
```

Painel de administração do RabbitMQ (ver filas, mensagens, etc):
```
http://localhost:15672
usuário: guest
senha:   guest
```

### Opção 2 — Rodando localmente (sem Docker para a aplicação)

Pré-requisitos:
- Java 21 instalado
- PostgreSQL e RabbitMQ rodando localmente (ou via Docker só para esses dois)

```bash
./mvnw spring-boot:run
```

> O perfil `local` (`application-local.properties`) já vem configurado para apontar para banco/fila em `localhost`.

---

## 📡 Endpoints da API

### 1. Enviar um PDF para processamento

```
POST /api/pdf/upload
Content-Type: multipart/form-data
```

**Parâmetro:** `file` (o arquivo PDF)

**Exemplo com `curl`:**
```bash
curl -X POST http://localhost:8080/api/pdf/upload \
  -F "file=@/caminho/do/arquivo.pdf"
```

**Resposta (HTTP 202 Accepted):**
```json
{
  "taskId": "f3a1c2b4-1234-5678-9abc-def012345678",
  "status": "PENDING",
  "pollUrl": "/api/pdf/result/f3a1c2b4-1234-5678-9abc-def012345678"
}
```

> Guarde o `taskId` — ele é usado para consultar o resultado depois.

---

### 2. Consultar o resultado de uma tarefa

```
GET /api/pdf/result/{taskId}
```

**Exemplo:**
```bash
curl http://localhost:8080/api/pdf/result/f3a1c2b4-1234-5678-9abc-def012345678
```

**Possíveis respostas:**

Enquanto está processando:
```json
{
  "taskId": "f3a1c2b4-...",
  "status": "PROCESSING",
  "extractedText": null
}
```

Quando termina com sucesso:
```json
{
  "taskId": "f3a1c2b4-...",
  "status": "DONE",
  "extractedText": "REPUBLICA FEDERATIVA DO BRASIL\nNome: JOAO DA SILVA\n..."
}
```

Quando falha:
```json
{
  "taskId": "f3a1c2b4-...",
  "status": "FAILED",
  "errorMessage": "Erro simulado de OCR"
}
```

> Como é assíncrono, você precisa **consultar esse endpoint algumas vezes** (polling) até o status mudar de `PENDING`/`PROCESSING` para `DONE` ou `FAILED`.

---

### 3. Ver o status geral do sistema (monitoramento)

```
GET /api/pdf/status
```

**Exemplo de resposta:**
```json
{
  "mainQueueSize": 3,
  "deadLetterQueueSize": 0,
  "jobsPending": 2,
  "jobsDone": 15,
  "jobsFailed": 1
}
```

Útil para saber quantas tarefas estão na fila, quantas já terminaram e quantas falharam.

---

## ⚙️ Sobre o processamento (Worker + OCR)

- O **`PdfWorker`** "escuta" a fila do RabbitMQ e, a cada PDF recebido, chama o serviço de OCR.
- Atualmente o OCR é **simulado** (`MockOcrService`) — ele não lê o PDF de verdade, apenas espera um tempo aleatório e retorna um texto de exemplo (simulando dados de um RG). Ele também falha aleatoriamente em ~10% das vezes, para simular erros reais.
- Isso é útil para testar o fluxo completo (fila, retries, falhas) **sem precisar de um OCR real** ainda.
- Quando uma tarefa falha, ela é enviada para uma **fila de "mensagens mortas" (Dead Letter Queue)**, evitando que fique tentando para sempre.

> 💡 Para usar OCR de verdade, basta criar uma nova implementação da interface `OcrService` e ativá-la no perfil de produção (`@Profile("prod")`).

---

## 🗃️ Banco de dados

A tabela principal é `pdf_jobs`, criada automaticamente pelo Hibernate (`ddl-auto=update`). Ela guarda:

| Campo | Descrição |
|---|---|
| `task_id` | ID único da tarefa (UUID) |
| `original_filename` | Nome do arquivo enviado |
| `status` | `PENDING`, `PROCESSING`, `DONE` ou `FAILED` |
| `extracted_text` | Texto extraído (quando concluído com sucesso) |
| `error_message` | Mensagem de erro (quando falhou) |
| `attempts` | Quantas vezes tentou processar |
| `created_at` / `updated_at` | Datas de criação/atualização |

---

## 🔧 Configurações importantes

| Variável de ambiente | Padrão (local) | Para quê serve |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/pdfapi` | Conexão com o PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `pdfuser` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `pdfpass` | Senha do banco |
| `SPRING_RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `SPRING_RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |

- Tamanho máximo de arquivo aceito no upload: **20MB**
- A fila tem TTL de 10 minutos (`x-message-ttl`) e limite de 10.000 mensagens.
- Tentativas automáticas de reprocessamento: até **3 vezes** em caso de erro, com espera crescente entre elas.

---

## ✅ Resumo rápido

1. `docker-compose up --build` para subir tudo.
2. `POST /api/pdf/upload` para enviar um PDF → recebe um `taskId`.
3. `GET /api/pdf/result/{taskId}` até o status virar `DONE` ou `FAILED`.
4. `GET /api/pdf/status` para ver o panorama geral do sistema.
