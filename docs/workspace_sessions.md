# AuraPay — Coordinatore delle Sessioni di Sviluppo (Workspace Master)

Questo documento funge da **punto di riferimento (Single Source of Truth)** per lo sviluppo modulare e controllato di AuraPay. Ogni sessione/chat con l'assistente IA si concentrerà su un singolo modulo o infrastruttura, seguendo questo piano per evitare deviazioni ("vibe coding").

---

## Protocollo per Nuove Sessioni (Come avviare una nuova chat)

Quando apri una nuova chat nel workspace in IntelliJ per iniziare una nuova sessione:
1. **Fornisci il contesto:** Menziona questo file `docs/workspace_sessions.md` e il `README.md` all'inizio della chat (es. `"Leggi docs/workspace_sessions.md per capire a che punto siamo e cosa fare in questa sessione"`).
2. **Obiettivo della sessione:** Indica chiaramente l'ID della sessione che intendi avviare (es. *"Iniziamo la Sessione 2: Sviluppo di aura-core-lib"*).
3. **Aggiornamento dello stato:** Alla fine di ogni sessione, l'agente dovrà aggiornare questo file impostando lo stato della sessione completata su `Completato [x]` e descrivendo brevemente cosa è stato fatto.

---

## Indice delle Sessioni di Sviluppo

Di seguito è riportato l'ordine sequenziale delle sessioni di sviluppo. Ciascuna sessione corrisponde a una chat verticale dedicata.

### 🟢 Sessione 1: Setup Progetto Generale & Infrastruttura Git
*   **Obiettivo:** Inizializzazione monorepo Maven parent, `.gitignore`, automazioni GitHub Actions, Docker Compose globale (Postgres, Redis, Kafka, MinIO) e questo file di coordinamento.
*   **Stato:** `Completato [x]`
*   **Risultato atteso:** Il progetto compila vuoto, l'infrastruttura locale parte con `docker compose up -d`, il repo Git è pronto per essere inviato a GitHub.

### 🟢 Sessione 2: aura-core-lib (Libreria Condivisa)
*   **Obiettivo:** Creazione del modulo condiviso. Definizione dei modelli degli eventi Kafka comuni (18 Java 21 Event Records), classi base per eccezioni, DTO di errore standard e security utility per la firma HMAC-SHA256, cifratura AES-256 GCM, mascheramento carte e hashing BCrypt API key. Creata guida allo studio `docs/module_structure_guide.md`.
*   **Stato:** `Completato [x]`
*   **Dipendenze:** Sessione 1

### 🟢 Sessione 3: Bank Simulator & API Gateway (Questa sessione)
*   **Obiettivo:** Setup di Spring Cloud Gateway reattivo (`aura-api-gateway`, porta 8080) per routing, CORS, tracciamento distributed tracing `X-Correlation-ID` e gestione unificata degli errori; e del simulatore bancario (`aura-bank-simulator`, porta 8086) con motore a regole deterministiche (Magic Rules *99, *98, *97, *95), latenza artificiale e pubblicazione eventi audit `BankAuthorizationResultEvent` su Kafka. Aggiornata la guida allo studio `docs/module_structure_guide.md`.
*   **Stato:** `Completato [x]`
*   **Dipendenze:** Sessione 2

### 🟢 Sessione 4: Vault Service (Tokenizzazione Carte)
*   **Obiettivo:** Sviluppo del servizio PCI-scoped fittizio che riceve i dati della carta e restituisce un token temporaneo, isolando i dati sensibili.
*   **Stato:** `Completato [x]`
*   **Risultato atteso:** Il servizio è integrato con HashiCorp Vault (Transit Secrets Engine) tramite un client HTTP leggero basato su `RestClient` con auto-bootstrap. La cifratura/decifratura è delegata a Vault e i dati mascherati sono persistiti su PostgreSQL (`aura_vault_db`). I token hanno TTL di 15 minuti e sono single-use.
*   **Dipendenze:** Sessione 2


### 🟢 Sessione 5: Payment Orchestrator (Happy Path Sincrono)
*   **Obiettivo:** Creazione del modulo `aura-payment-orchestrator`. Macchina a stati `PaymentIntent` (`CREATED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED`), orchestrazione sincrona tra `aura-vault-service` (detokenizzazione) e `aura-bank-simulator` (autorizzazione bancaria), REST Controller su `/v1/payments`, gestione errori con `ErrorResponse` e suite di test JUnit 5.
*   **Stato:** `Completato [x]`
*   **Dipendenze:** Sessione 3, Sessione 4

### 🟢 Sessione 6: Debezium & Outbox Pattern (Event-Driven Engine)
*   **Obiettivo:** Integrazione del Transactional Outbox Pattern in `Aura-Payment-Orchestrator`. Creazione tabella `outbox_events`, entità JPA `OutboxEvent`, repository e `PaymentEventFactory` per serializzazione DTO eventi da `aura-core-lib`. Configurazione di Debezium CDC (`docker/debezium/outbox-connector.json`) per il routing degli eventi outbox sui topic Kafka.
*   **Stato:** `Completato [x]`
*   **Dipendenze:** Sessione 5


### ⚪ Sessione 7: Ledger Service (Partita Doppia)
*   **Obiettivo:** Sviluppo del servizio contabile. Consuma gli eventi di pagamento e rimborso, registrando le righe Dare/Avere in modo immutabile.
*   **Stato:** `Non Iniziato [ ]`
*   **Dipendenze:** Sessione 6

### ⚪ Sessione 8: Merchant & Webhook Services
*   **Obiettivo:** Gestione dei merchant, chiavi API (test/live), flusso di onboarding e sistema asincrono di notifica webhook via HMAC con retry esponenziale.
*   **Stato:** `Non Iniziato [ ]`
*   **Dipendenze:** Sessione 6

### ⚪ Sessione 9: Invoice Service (Fatturazione PDF & MinIO S3)
*   **Obiettivo:** Sviluppo del servizio di fatturazione. Genera i PDF delle fatture/note di credito e li carica su MinIO, esponendo URL firmati a tempo.
*   **Stato:** `Non Iniziato [ ]`
*   **Dipendenze:** Sessione 6

### ⚪ Sessione 10: E2E Integration Testing & Polish Backend
*   **Obiettivo:** Creazione di una suite completa di test E2E (es. via script o test di integrazione Spring Boot) per verificare l'intero flusso del backend senza frontend. Polish del codice del backend.
*   **Stato:** `Non Iniziato [ ]`
*   **Dipendenze:** Sessione 7, Sessione 8, Sessione 9

### ⚪ Sessione 11: React Frontend (Dashboard)
*   **Obiettivo:** Sviluppo dell'applicazione frontend React + TS + Vite. Connessione a tutti gli endpoint del backend (onboarding, storico transazioni, rimborsi, fatture, webhooks).
*   **Stato:** `Non Iniziato [ ]`
*   **Dipendenze:** Sessione 10

### ⚪ Sessione 12: Observability, Tracing & Showcase Finale
*   **Obiettivo:** Setup di OpenTelemetry, Prometheus, Grafana e Zipkin per il tracing distribuito. Conclusione del `README.md` finale con badge, screenshot della dashboard, e guida al portfolio per LinkedIn.
*   **Stato:** `Non Iniziato [ ]`
*   **Dipendenze:** Sessione 11

---

## Note e Regole di Sviluppo
*   **Nessuna deviazione di scope:** Non implementare feature esterne alla sessione corrente a meno che non siano bloccanti o richieste esplicitamente.
*   **Test First o Parallel:** Ogni servizio deve essere accompagnato da opportuni unit/integration test prima di essere considerato "Completato".
*   **Clean Code:** Seguire i pattern SOLID, DDD (Domain-Driven Design) per la logica di business e Clean Architecture.
