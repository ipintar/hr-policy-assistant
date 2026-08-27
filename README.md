# Policy Assistant

`Policy Assistant` je mali, ali funkcionalan Spring Boot RAG primjer za HR policy pitanja. Aplikacija:

- učitava lokalni knowledge base iz JSON-a
- reže policy sadržaj na chunkove
- sprema embeddinge u PostgreSQL s `pgvector`
- radi semantic retrieval nad chunkovima
- generira odgovor preko OpenAI modela
- vraća answer, citations i retrieved chunks preko REST API-ja

## Stack

- Java 21
- Spring Boot 3.5
- Spring AI
- OpenAI chat + embeddings
- PostgreSQL + pgvector
- Swagger / OpenAPI
- Docker Compose za lokalnu bazu

## API endpointi

- `POST /api/policies/ask`
- `GET /api/policies/index/status`
- `POST /api/policies/index/rebuild`
- `GET /actuator/health`
- `GET /swagger-ui.html`

## Preduvjeti

- JDK 21
- Docker Desktop
- OpenAI API key

## 1. Pokreni bazu

U rootu projekta pokreni:

```powershell
docker compose up -d
```

Ovo diže lokalni PostgreSQL s uključenim `pgvector` extensionom.

Provjera:

```powershell
docker ps
```

Trebao bi vidjeti container `policy-assistant-pgvector`.

## 2. Postavi environment varijable

Primjer vrijednosti je u [.env.example](/C:/Users/ivan.pintar/Projects/private/policy-assistant/.env.example:1).

U PowerShellu:

```powershell
$env:OPENAI_API_KEY="your-openai-api-key"
$env:OPENAI_CHAT_MODEL="gpt-4.1-mini"
$env:OPENAI_EMBEDDING_MODEL="text-embedding-3-small"
```

Po želji možeš promijeniti i DB vrijednosti:

```powershell
$env:POLICY_DB_URL="jdbc:postgresql://localhost:5432/policy_assistant"
$env:POLICY_DB_USERNAME="policy_assistant"
$env:POLICY_DB_PASSWORD="policy_assistant"
```

## 3. Pokreni aplikaciju

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="C:\Program Files\Java\jdk-21\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Pri startupu aplikacija:

- učita knowledge base iz `src/main/resources/policies/policy-knowledge-base.json`
- chunka dokumente
- generira embeddinge
- upisuje chunkove u `vector_store`

## 4. Otvori Swagger

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/api-docs)
- [Actuator health](http://localhost:8080/actuator/health)

## Primjer requesta

`POST /api/policies/ask`

```json
{
  "question": "How many vacation days do employees have?"
}
```

Primjer odgovora:

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

## Operativne napomene

- `POST /api/policies/index/rebuild` ručno reindeksira knowledge base
- `GET /api/policies/index/status` vraća status indeksiranja
- `POLICY_RAG_ENABLED=false` gasi RAG endpointe i indexing
- `POLICY_RAG_INDEX_ON_STARTUP=false` preskače automatski startup reindex

## Build

Compile:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="C:\Program Files\Java\jdk-21\bin;$env:Path"
.\mvnw.cmd "-Dmaven.repo.local=.m2repo" compile
```

Package:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="C:\Program Files\Java\jdk-21\bin;$env:Path"
.\mvnw.cmd "-Dmaven.repo.local=.m2repo" package
```

## Docker image

Ako želiš izgraditi aplikacijski image:

```powershell
docker build -t policy-assistant .
```

## Trenutna ograničenja

- knowledge base je lokalni JSON, nije admin CMS
- nema autentikacije
- nema conversation memory
- nema evaluacijskog seta ni testova za retrieval kvalitetu
- indeksiranje trenutno radi full rebuild umjesto incremental updatea
