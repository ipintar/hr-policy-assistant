# Policy Assistant

Policy Assistant is a Spring Boot reference application for answering HR policy questions with retrieval-augmented generation (RAG).

The application loads policy documents from a local JSON knowledge base, splits them into searchable chunks, stores their embeddings in PostgreSQL with `pgvector`, retrieves the most relevant context for each question, and uses an OpenAI chat model to generate a grounded answer with citations.

## Features

- REST API for asking natural-language HR policy questions
- Semantic search backed by PostgreSQL and `pgvector`
- OpenAI chat and embedding model integration through Spring AI
- Policy citations and retrieved excerpts in API responses
- Configurable chunking, retrieval, and startup indexing
- Index status and manual rebuild endpoints
- OpenAPI documentation and Swagger UI
- Health checks through Spring Boot Actuator
- Docker Compose setup for the local vector database

## Technology stack

- Java 21
- Spring Boot 3.5
- Spring AI
- OpenAI chat and embedding models
- PostgreSQL 16 with `pgvector`
- Springdoc OpenAPI
- Maven Wrapper
- Docker Compose

## How it works

1. Policy documents are loaded from `src/main/resources/policies/policy-knowledge-base.json`.
2. Each document is normalized and split into overlapping chunks.
3. Spring AI generates embeddings and stores the chunks in the `vector_store` table.
4. A question is embedded and matched against the most relevant policy chunks.
5. The retrieved context and question are sent to the configured chat model.
6. The API returns the generated answer, citations, and retrieved excerpts.

## Prerequisites

- JDK 21
- Docker with Docker Compose
- An OpenAI API key

No separate Maven installation is required because the repository includes the Maven Wrapper.

## Configuration

The application is configured with environment variables. See [`.env.example`](.env.example) for an example.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `OPENAI_API_KEY` | Yes | None | API key used for chat completions and embeddings |
| `OPENAI_CHAT_MODEL` | No | `gpt-4.1-mini` | OpenAI model used to generate answers |
| `OPENAI_EMBEDDING_MODEL` | No | `text-embedding-3-small` | OpenAI model used to create embeddings |
| `POLICY_DB_URL` | No | `jdbc:postgresql://localhost:5432/policy_assistant` | PostgreSQL JDBC URL |
| `POLICY_DB_USERNAME` | No | `policy_assistant` | PostgreSQL username |
| `POLICY_DB_PASSWORD` | No | `policy_assistant` | PostgreSQL password |
| `POLICY_RAG_ENABLED` | No | `true` | Enables policy retrieval and question answering |
| `POLICY_RAG_INDEX_ON_STARTUP` | No | `true` | Rebuilds the vector index when the application starts |
| `SERVER_PORT` | No | `8080` | HTTP port used by the application |

Set the required API key before starting the application.

PowerShell:

```powershell
$env:OPENAI_API_KEY="your-openai-api-key"
```

macOS or Linux:

```bash
export OPENAI_API_KEY="your-openai-api-key"
```

## Running locally

### 1. Start PostgreSQL

From the repository root, run:

```bash
docker compose up -d
```

Docker Compose starts a PostgreSQL instance with the `vector` extension enabled. The database is exposed on port `5432` and its data is persisted in a named Docker volume.

Check the service status with:

```bash
docker compose ps
```

### 2. Start the application

PowerShell or Command Prompt:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
./mvnw spring-boot:run
```

When startup indexing is enabled, the application loads the bundled knowledge base, generates embeddings, and rebuilds the vector index before serving policy questions.

### 3. Explore the API

After the application starts, the following resources are available:

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI specification](http://localhost:8080/api-docs)
- [Application health](http://localhost:8080/actuator/health)

## API endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/policies/ask` | Answers a policy question using retrieved policy context |
| `GET` | `/api/policies/index/status` | Returns the current indexing status |
| `POST` | `/api/policies/index/rebuild` | Rebuilds the vector index from the configured knowledge base |
| `GET` | `/actuator/health` | Returns application health information |

### Ask a policy question

```bash
curl --request POST http://localhost:8080/api/policies/ask \
  --header "Content-Type: application/json" \
  --data '{"question":"How many vacation days do employees have?"}'
```

Example response:

```json
{
  "question": "How many vacation days do employees have?",
  "answer": "Employees receive 25 paid vacation days per calendar year according to the Vacation Leave Policy.",
  "model": "gpt-4.1-mini",
  "retrievalStrategy": "pgvector-openai-rag",
  "citations": [
    {
      "policyId": "vacation-policy",
      "title": "Vacation Leave Policy",
      "source": "hr-policy-handbook/vacation",
      "chunkIndex": 0
    }
  ],
  "retrievedChunks": [
    {
      "policyId": "vacation-policy",
      "title": "Vacation Leave Policy",
      "source": "hr-policy-handbook/vacation",
      "chunkIndex": 0,
      "excerpt": "Vacation leave is granted to full-time employees..."
    }
  ]
}
```

Questions must contain non-whitespace text and may be up to 2,000 characters long.

## Managing the knowledge base

The bundled knowledge base is stored in `src/main/resources/policies/policy-knowledge-base.json`. Each policy uses the following structure:

```json
{
  "id": "vacation-policy",
  "title": "Vacation Leave Policy",
  "source": "hr-policy-handbook/vacation",
  "content": "Policy text to index and retrieve."
}
```

After changing the knowledge base, restart the application with startup indexing enabled or rebuild the index manually:

```bash
curl --request POST http://localhost:8080/api/policies/index/rebuild
```

## Building and testing

PowerShell or Command Prompt:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
```

macOS or Linux:

```bash
./mvnw clean test
./mvnw package
```

The packaged application is written to the `target` directory.

## Building a Docker image

Build the application image from the repository root:

```bash
docker build -t policy-assistant .
```

The included Compose file starts only PostgreSQL. Run the application image separately and provide the required database and OpenAI environment variables.

## Current limitations

- Policy content is loaded from a bundled JSON file rather than a document management system.
- API endpoints do not currently require authentication or authorization.
- Conversations are stateless and do not retain message history.
- Retrieval quality is not yet measured against an automated evaluation dataset.
- Indexing performs a full rebuild rather than incremental updates.
- The project currently provides a backend API only and does not include a user interface.

This project is intended as a reference implementation and should be reviewed for security, privacy, access control, observability, and retrieval quality before production use.
