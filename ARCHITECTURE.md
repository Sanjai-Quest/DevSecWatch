# DevSecWatch Architecture

```mermaid
graph TD
    User[User Browser]
    
    subgraph Frontend [React Frontend]
        UI[Dashboard UI]
        AuthUI[Login/Register]
    end
    
    subgraph Backend [Spring Boot API]
        Controller[REST Controllers]
        AuthService[Auth Service]
        ScanService[Scan Service]
        DB[(PostgreSQL)]
    end
    
    subgraph Messaging
        RabbitMQ[RabbitMQ Queue]
    end
    
    subgraph Worker [Worker Service]
        Listener[Queue Listener]
        Git[Git Clone Service]
        Semgrep[Semgrep Analysis]
        Enricher[AI Enrichment]
    end
    
    subgraph AI [AI Service]
        FastAPI[Python FastAPI]
        LLM[Groq LLM API]
    end
    
    subgraph Caching
        Redis[(Redis Cache)]
    end

    User -->|HTTPS| Frontend
    Frontend -->|REST API| Backend
    Backend -->|Read/Write| DB
    Backend -->|Publish Job| RabbitMQ
    
    RabbitMQ -->|Consume Job| Worker
    Worker -->|Clone| Git
    Worker -->|Scan| Semgrep
    
    Worker -->|Check Cache| Redis
    Worker -->|Request Explanation| AI
    AI -->|Generate| LLM
    
    Worker -->|Save Results| DB
```

## Data Flow
1.  User submits a repository URL via Frontend.
2.  Backend validates request and saves `Scan` (QUEUED) to DB.
3.  Backend publishes message to RabbitMQ.
4.  Worker consumes message.
5.  Worker clones repository to temp storage.
6.  Worker runs `semgrep` to identify vulnerabilities.
7.  Worker calls AI Service (checking Redis first) to get fixes.
8.  Worker saves findings to DB and updates Scan status (COMPLETED).
9.  Frontend polls Backend and displays results.
