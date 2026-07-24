# Guida allo Studio e Spiegazione dell'Architettura dei Moduli (AuraPay)

Questo documento funge da **manuale architetturale dinamico** per lo studio e la spiegazione della codebase di AuraPay. Viene aggiornato al termine di ciascuna sessione di sviluppo verticale per riflettere la struttura dei pacchetti, le classi create, le motivazioni di design e i concetti chiave utili per interviste tecniche o code review.

---

## Indice dei Moduli
1. [aura-core-lib (Libreria Condivisa) — Sessione 2](#1-aura-core-lib-libreria-condivisa---sessione-2)
2. [aura-api-gateway & aura-bank-simulator — Sessione 3](#2-aura-api-gateway--aura-bank-simulator---sessione-3)
3. [aura-vault-service (Tokenizzazione Carte) — Sessione 4](#3-aura-vault-service-tokenizzazione-carte---sessione-4)
4. [aura-payment-orchestrator (Happy Path Sincrono) — Sessione 5](#4-aura-payment-orchestrator-happy-path-sincrono---sessione-5)
5. [Debezium & Outbox Pattern (Event-Driven Engine) — Sessione 6](#5-debezium--outbox-pattern-event-driven-engine---sessione-6)
6. [Convenzioni di Codice & Standard Ecosistema](#6-convenzioni-di-codice--standard-ecosistema)

---

## 1. `aura-core-lib` (Libreria Condivisa) — Sessione 2

### Scopo del Modulo
`aura-core-lib` è la libreria Java condivisa da tutti i microservizi dell'ecosistema AuraPay. Definisce:
1. **Contratti degli Eventi Kafka**: DTO immutabili standard per l'architettura Event-Driven.
2. **Gerarchia Eccezioni & DTO di Errore**: Standard per la gestione uniforme degli errori HTTP/Business in tutti i microservizi.
3. **Utility di Sicurezza e Crittografia**: Firma HMAC SHA-256, cifratura carte AES-256 GCM, mascheramento PAN e hashing API Key.

---

### Struttura dei Pacchetti e dei File

```
aura-core-lib/
└── src/
    ├── main/java/com/aurapay/core/
    │   ├── constants/
    │   │   └── AuraHeaders.java                          # Costanti per header HTTP standard (X-Correlation-ID, Idempotency-Key)
    │   │
    │   ├── events/
    │   │   ├── DomainEvent.java                          # Interfaccia base per tutti gli eventi
    │   │   ├── EventType.java                            # Enum dei 18 eventi + costanti Topic Kafka
    │   │   ├── MerchantCreatedEvent.java                 # Record evento registrazione merchant
    │   │   ├── MerchantVerifiedEvent.java                # Record evento KYB approvato
    │   │   ├── MerchantVerificationRejectedEvent.java   # Record evento KYB respinto
    │   │   ├── ApiKeyCreatedEvent.java                   # Record evento creazione API Key
    │   │   ├── ApiKeyRevokedEvent.java                   # Record evento revoca API Key
    │   │   ├── PaymentIntentCreatedEvent.java            # Record evento creazione PaymentIntent
    │   │   ├── PaymentProcessingEvent.java               # Record evento inizio autorizzazione
    │   │   ├── PaymentSucceededEvent.java                # Record evento pagamento completato
    │   │   ├── PaymentFailedEvent.java                   # Record evento pagamento fallito
    │   │   ├── RefundRequestedEvent.java                 # Record evento richiesta rimborso
    │   │   ├── RefundSucceededEvent.java                 # Record evento rimborso completato
    │   │   ├── RefundFailedEvent.java                    # Record evento rimborso fallito
    │   │   ├── InvoiceGeneratedEvent.java                # Record evento generazione PDF fattura
    │   │   ├── InvoiceGenerationFailedEvent.java         # Record evento errore fattura
    │   │   ├── WebhookDeliverySucceededEvent.java        # Record evento consegna webhook OK
    │   │   ├── WebhookDeliveryDeadLetteredEvent.java     # Record evento esaurimento retry webhook
    │   │   ├── LedgerEntryRecordedEvent.java             # Record evento scrittura contabile
    │   │   └── BankAuthorizationResultEvent.java         # Record evento esito banca
    │   │   ├── BankResponseCode.java                     # Enum dei codici esito bancario ISO 8583 (00, 51, 54, 59, 96)
    │   │
    │   ├── exception/
    │   │   ├── AuraErrorCode.java                        # Enum centralizzata dei codici di errore di dominio
    │   │   ├── AuraException.java                        # RuntimeException radice del sistema
    │   │   ├── BusinessException.java                    # Eccezione base per errori di business
    │   │   ├── ResourceNotFoundException.java            # Entità non trovata (404)
    │   │   ├── UnauthorizedException.java                # Errore autenticazione generico (401)
    │   │   ├── InvalidApiKeyException.java               # API Key non valida / revocata (401)
    │   │   ├── DomainRuleViolationException.java         # Regola di dominio violata (422)
    │   │   ├── IdempotencyConflictException.java         # Concorrenza su Idempotency Key (409)
    │   │   ├── CryptoException.java                      # Errore operazione crittografica
    │   │   ├── ErrorResponse.java                        # DTO risposta HTTP di errore standard
    │   │   └── FieldErrorDto.java                        # DTO errore di validazione singoli campi
    │   │
    │   └── security/
    │       ├── HmacUtils.java                            # HMAC SHA-256 (firme webhook e presigned URL)
    │       ├── AESCryptoUtils.java                       # AES-256 GCM cifratura/decifratura simmetrica
    │       ├── CardMaskingUtils.java                     # Mascheramento PAN (es. 453201****1111)
    │       └── ApiKeyGenerator.java                      # Generatore API key (test/live) & hashing BCrypt
    │
    └── test/java/com/aurapay/core/
        ├── events/
        │   └── EventSerializationTest.java               # Test serializzazione/deserializzazione Jackson Records
        └── security/
            ├── HmacUtilsTest.java                        # Test firma e confronto a tempo costante HMAC
            ├── AESCryptoUtilsTest.java                   # Test cifratura AES GCM con IV dinamico
            ├── CardMaskingUtilsTest.java                 # Test mascheramento PAN e BIN extraction
            └── ApiKeyGeneratorTest.java                  # Test generazione e hashing BCrypt API Keys
```

---

### Dettaglio delle Classi e delle Scelte di Design

#### 1. `com.aurapay.core.events`
* **`DomainEvent`**: Interfaccia comune per polimorfismo ed elaborazione generica di tutti gli eventi. Espone `getEventId()`, `getEventType()`, `getOccurredAt()`, `isTest()` e il metodo di utilità `getEventTypeEnum()` (annotato con `@JsonIgnore`).
* **`EventType`**: Enum che definisce la sorgente unica dei nomi dei topic Kafka (`aura.payment.succeeded.v1`, ecc.). Include il metodo di ricerca statico `fromTopicName(String)`.
* **Perché `String getEventType()` invece di `EventType`?**
  1. **Disaccoppiamento del Protocollo di Rete**: Gli eventi Kafka viaggiano su rete come stringhe JSON (`"aura.payment.succeeded.v1"`).
  2. **Tolleranza ai Cambiamenti di Schema (Backward/Forward Compatibility)**: Se un microservice consumer riceve un evento nuovo con un tipo non ancora presente nel suo enum locale `EventType`, restituire una `String` permette di deserializzare e gestire o ignorare l'evento senza causare `InvalidFormatException` in Jackson.
  3. **Ibrido Elegante**: Per la convenienza del codice Java, `DomainEvent` fornisce il metodo helper default `@JsonIgnore EventType getEventTypeEnum()`, che converte la stringa nel tipo fortemente tipizzato se riconosciuto.
* **Java 21 Records (18 Event DTOs)**:
  * **Immutabilità Garantita**: Gli eventi sono fatti storici avvenuti nel passato. I Records impediscono modifiche accidentali del payload a runtime.
  * **Thread-Safety nativa**: Perfetti per il consumo concorrente tramite Virtual Threads in Spring Boot 3.3+.
  * **Serializzazione Jackson**: Interagiscono nativamente con `ObjectMapper` tramite `JavaTimeModule` per le date `Instant`.

#### 2. `com.aurapay.core.exception`
* **`AuraException`**: Radice della gerarchia delle eccezioni uncheck. Perrore di design: estende `RuntimeException` per evitare dichiarazioni `throws` verbose sui metodi di dominio.
* **`BusinessException`**: Introduce un `errorCode` stringa (es. `RESOURCE_NOT_FOUND`, `UNAUTHORIZED`) associato all'eccezione, mappabile direttamente nei controller Spring via `@RestControllerAdvice`.
* **`ErrorResponse` & `FieldErrorDto`**: DTO immutabili che garantiscono che ogni microservizio risponda agli errori con la stessa identica struttura JSON standard:
  ```json
  {
    "timestamp": "2026-07-20T18:20:00Z",
    "status": 404,
    "error": "RESOURCE_NOT_FOUND",
    "message": "PaymentIntent with id 'pi_123' was not found",
    "path": "/v1/payments/pi_123",
    "fieldErrors": []
  }
  ```

#### 3. `com.aurapay.core.security`
* **`HmacUtils`**:
  * Utilizza l'algoritmo **HMAC-SHA256**.
  * **Sicurezza Anti-Timing Attack**: La verifica della firma non usa `.equals()` tra stringhe (che restituirebbe `false` al primo carattere diverso, lasciando trapelare informazioni sulla lunghezza del prefisso valido), ma `MessageDigest.isEqual()`, che confronta gli array di byte a tempo costante.
* **`AESCryptoUtils`**:
  * Implementa **AES-256 GCM** (Galois/Counter Mode).
  * A differenza di CBC, GCM è una modalità di cifratura **autenticata** (Fornisce sia riservatezza sia integrità tramite un tag di autenticazione a 128 bit).
  * Per ogni cifratura viene generato un **IV (Initialization Vector) casuale a 12 bit (96-bit)** via `SecureRandom`. L'IV viene concatenato in testa al ciphertext e codificato in Base64 `[12-byte IV + Ciphertext]`.
* **`CardMaskingUtils`**:
  * Estrae il BIN (primi 6 numeri) e le ultime 4 cifre, mascherando con asterischi i numeri intermedi. Conforme alle linee guida PCI-DSS per l'esibizione di PAN nei log o nei report non protetti.
* **`ApiKeyGenerator`**:
  * Genera API Key con prefissi definiti (`pk_test_`, `sk_test_`, `pk_live_`, `sk_live_`).
  * Fornisce hashing **BCrypt** (`BCryptPasswordEncoder`) per salvare il secret hashato nel database senza conservare mai API key in chiaro.

---

### Spunti di Discussione per Colloqui / Technical Reviews
* **Q: Perché usate i Java Records e non `@Data` / POJO Lombok per i DTO degli eventi?**
  * *A*: Gli eventi di dominio rappresentano fatti storici immutabili. Un Record impedisce strutturalmente la presenza di setter o la mutazione dello stato dopo l'istanziazione, garantendo thread-safety nativa e zero overhead di boilerplate senza dipendenze da plugin di compilazione terzi.
* **Q: Come prevenite i timing attacks nelle verifiche delle firme Webhook?**
  * *A*: Utilizziamo `MessageDigest.isEqual()` in `HmacUtils` per confrontare le firme generata ed attesa a tempo costante, eliminando la vulnerabilità dovuta alla terminazione anticipata dei confronti di stringhe standard.
* **Q: Perché avete scelto AES GCM invece di AES CBC per la cifratura dei dati at-rest?**
  * *A*: AES GCM fornisce Authentic Encryption (AEAD). Oltre a cifrare il dato, include un tag di autenticazione a 128 bit che protegge il payload da qualsiasi manomissione o bit-flipping prima della decifratura.

---

## 2. `aura-api-gateway` & `aura-bank-simulator` — Sessione 3

### Scopo dei Moduli
In questa sessione sono stati creati i due microservizi di ingresso e di simulazione bancaria:

1. **`aura-api-gateway` (Spring Cloud Gateway - Reactive WebFlux)**:
   * **Single Point of Entry**: Esposto sulla porta `8080`, gestisce il routing dinamico di tutte le chiamate REST dirette ai microservizi downstream (`/v1/merchants/**`, `/v1/payments/**`, `/v1/invoices/**`, `/v1/bank/**`).
   * **Distributed Tracing Header**: Genera e propaga l'header `X-Correlation-ID` su tutte le richieste in ingresso ed in uscita verso i microservizi.
   * **Global CORS & Standard Error Translation**: Configurazione centralizzata CORS per la SPA React e gestore eccezioni reattivo (`GlobalErrorWebExceptionHandler`) per risposte HTTP d'errore uniformi (`ErrorResponse`).

2. **`aura-bank-simulator` (Spring Boot 3.x REST Microservice)**:
   * **Acquiring Bank Simulator**: Esposto sulla porta `8086`, simula le risposte di autorizzazione delle reti di pagamento e di gestione rimborsi.
   * **Magic Rules Engine**: Regole deterministiche per simulare fondi insufficienti (`*99`), carta scaduta (`*98`), frode (`*97`), timeout di rete (`*95`) o approvazione immediata.
   * **Audit Event Publishing**: Pubblica l'evento `BankAuthorizationResultEvent` (da `aura-core-lib`) sul topic Kafka `aura.bank.authorization_result.v1`.

---

### Struttura dei Pacchetti e dei File

```
aura-api-gateway/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/aurapay/gateway/
    │   │   ├── AuraApiGatewayApplication.java            # Main application class
    │   │   ├── config/
    │   │   │   └── CorsConfig.java                        # Configurazione CORS Reactive WebFilter
    │   │   ├── exception/
    │   │   │   └── GlobalErrorWebExceptionHandler.java    # Exception Handler WebFlux -> ErrorResponse DTO
    │   │   └── filter/
    │   │       └── CorrelationIdFilter.java               # Global Gateway Filter per X-Correlation-ID
    │   └── resources/
    │       └── application.yml                            # Route definitions & Redis config
    └── test/
        └── java/com/aurapay/gateway/
            └── AuraApiGatewayApplicationTests.java        # Spring Cloud Gateway WebTestClient integration test

aura-bank-simulator/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/aurapay/banksimulator/
    │   │   ├── AuraBankSimulatorApplication.java         # Main application class
    │   │   ├── controller/
    │   │   │   └── BankSimulatorController.java          # Endpoint /v1/bank/authorize & /v1/bank/refund
    │   │   ├── dto/
    │   │   │   ├── BankAuthorizationRequest.java          # Request payload autorizzazione
    │   │   │   ├── BankAuthorizationResponse.java         # Response payload autorizzazione
    │   │   │   ├── BankRefundRequest.java                 # Request payload rimborso
    │   │   │   └── BankRefundResponse.java                # Response payload rimborso
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java            # RestControllerAdvice -> ErrorResponse DTO
    │   │   ├── publisher/
    │   │   │   └── BankEventPublisher.java                # Componente dedicato all'invio eventi Kafka
    │   │   ├── service/
    │   │   │   └── BankSimulatorService.java              # Magic rules engine per autorizzazioni e rimborsi
    │   │   └── util/
    │   │       └── LatencyUtils.java                      # Utility final per simulazione latenza di rete
    │   └── resources/
    │       └── application.yml                            # Port 8086, Kafka bootstrap & latency properties
    └── test/
        └── java/com/aurapay/banksimulator/
            ├── controller/
            │   └── BankSimulatorControllerTest.java       # WebMvcTest per validazione DTO ed HTTP responses
            └── service/
                └── BankSimulatorServiceTest.java          # Unit test regole deterministiche & eccezioni
```

---

### Dettaglio delle Classi e delle Scelte di Design

#### 1. `com.aurapay.gateway` (API Gateway)
* **Perché Spring Cloud Gateway (WebFlux / Netty) rispetto a Spring MVC / Tomcat?**
  * Spring Cloud Gateway si basa sul paradigm **Non-Blocking I/O (Reactive WebFlux)**.
  * In un API Gateway che instrada migliaia di chiamate I/O concorrenti verso microservizi backend, l'architettura reattiva evita il blocco dei thread del server (thread starvation), garantendo footprint di memoria minimo ed elevatissimo throughput.
* **`CorrelationIdFilter`**:
  * Implementa `GlobalFilter` e `Ordered` con priorità `HIGHEST_PRECEDENCE`.
  * Ispeziona l'header `X-Correlation-ID`. Se mancante, ne genera uno nuovo via `UUID.randomUUID().toString()`.
  * Inietta l'header sia nella richiesta in corso diretta ai microservizi downstream (`request.mutate().header(...)`), sia nella risposta HTTP diretta al client (`response.getHeaders().add(...)`).
* **`GlobalErrorWebExceptionHandler`**:
  * Implementa `ErrorWebExceptionHandler` per catturare qualsiasi eccezione generata all'interno della pipeline del Gateway (es. 404 Route Not Found, 429 Rate Limited, 503 Service Unavailable).
  * Converte lo stato HTTP ed il messaggio di errore nel formato JSON standard `ErrorResponse` di `aura-core-lib`, restituendo `application/json` con UTF-8 encoding.

#### 2. `com.aurapay.banksimulator` (Bank Simulator)
* **`BankSimulatorService`**:
  * **Algoritmo Magic Rules**: Analizza l'importo della transazione `amountCents` tramite l'operatore modulo 100 (`amountCents % 100`).
    * `99` -> Rifiutato per `INSUFFICIENT_FUNDS` (`responseCode: 51`).
    * `98` -> Rifiutato per `EXPIRED_CARD` (`responseCode: 54`).
    * `97` -> Rifiutato per `SUSPECTED_FRAUD` (`responseCode: 59`).
    * `95` -> Timeout simulato (`BusinessException` con codice `BANK_UNAVAILABLE`).
    * Qualsiasi altro importo -> Approvato (`responseCode: 00`, `authorized: true`, genera `transactionId` univoco `tx_bank_...` ed `authorizationCode` `AUTH_...`).
  * **Simulazione Latenza Netted**: Configurato tramite `${aurapay.bank.simulated-latency-ms:100}` per riprodurre in modo controllato i tempi di trasmissione di rete verso circuiti internazionali.
  * **Pubblicazione Eventi di Audit**: Ogni tentativo di autorizzazione invia un messaggio `BankAuthorizationResultEvent` su Kafka topic `aura.bank.authorization_result.v1`.
* **`GlobalExceptionHandler` (`@RestControllerAdvice`)**:
  * Intercetta `BusinessException` e `MethodArgumentNotValidException`, mappando i messaggi ed eventuali `FieldErrorDto` nel formato `ErrorResponse`.

---

### Spunti di Discussione per Colloqui / Technical Reviews
* **Q: Perché il Gateway è realizzato con Spring Cloud Gateway Reattivo anziché un normale controller Spring MVC?**
  * *A*: L'API Gateway è il bottleneck di ingresso dell'intero sistema. Usare il modello reattivo non-blocking (Netty + Reactor) consente di gestire un numero elevatissimo di connessioni HTTP simultanee mantenendo bassissimo l'uso di RAM e la CPU, senza riservare un thread per ciascuna richiesta pendente.
* **Q: Come tracciate una richiesta che attraversa molteplici microservizi partendo dal Gateway?**
  * *A*: Attraverso il `CorrelationIdFilter`, il Gateway genera (o mantiene) un header unico `X-Correlation-ID`. Tutti i microservizi trasmettono questo header nelle loro chiamate HTTP downstream e nei log (MDC), permettendo di correlare istantaneamente tutti i log appartenenti ad un singola transazione.
* **Q: Come funziona il meccanismo di Magic Numbers nel Bank Simulator?**
  * *A*: Per evitare di introdurre dipendenze da banche esterne o chiavi fittizie complesse, utilizziamo gli ultimi due numeri dell'importo in centesimi per pilotare in modo deterministico l'esito della transazione (es. 10.99€ simula fondi insufficienti, 10.95€ simula timeout). Questo approccio rende estremamente semplice ed immediata la scrittura di test automatici E2E per casi negativi ed edge-cases.

---

## 3. `aura-vault-service` (Tokenizzazione Carte) — Sessione 4

### Scopo del Modulo
`aura-vault-service` è il servizio PCI-scoped responsabile dell'isolamento dei dati di pagamento sensibili (PAN e CVV). Esso fornisce:
1. **Tokenizzazione delle Carte**: Riceve i dati della carta in chiaro, convalida formalmente il numero di carta tramite l'algoritmo di Luhn, cifra i dati sensibili delegando la cifratura a HashiCorp Vault e restituisce un token temporaneo (`tok_...`).
2. **Detokenizzazione Sicura**: Consente ai soli servizi interni autorizzati (come l'orchestratore) di recuperare i dati della carta in chiaro a partire dal token, contrassegnando immediatamente il token come utilizzato (vincolo single-use).

---

### Struttura dei Pacchetti e dei File

```
aura-vault-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/aurapay/vault/
    │   │   ├── AuraVaultApplication.java                 # Classe di ingresso Spring Boot
    │   │   │
    │   │   ├── client/
    │   │   │   └── VaultClient.java                       # Client REST per HashiCorp Vault Transit Engine
    │   │   │
    │   │   ├── controller/
    │   │   │   └── VaultController.java                   # Endpoint POST /v1/tokens e /v1/tokens/retrieve
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── TokenizeRequest.java                   # DTO di richiesta per la tokenizzazione
    │   │   │   ├── TokenResponse.java                     # DTO di risposta per la tokenizzazione
    │   │   │   ├── RetrieveRequest.java                   # DTO di richiesta per il recupero dati
    │   │   │   └── CardDetailsResponse.java               # DTO di risposta per il recupero dati
    │   │   │
    │   │   ├── entity/
    │   │   │   └── CardToken.java                         # Entità JPA per mappare il token e i metadati della carta
    │   │   │
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java            # RestControllerAdvice per la gestione uniforme degli errori
    │   │   │
    │   │   ├── repository/
    │   │   │   └── CardTokenRepository.java               # Repository per la persistenza dei token
    │   │   │
    │   │   ├── service/
    │   │   │   └── VaultService.java                      # Core business logic (Luhn, brand detection, TTL, Vault interaction)
    │   │   │
    │   │   └── validator/
    │   │       └── LuhnValidator.java                     # Algoritmo di Luhn per validazione formale del PAN
    │   │
    │   └── resources/
    │       ├── application.yml                            # Configurazione porta, DB Postgres e parametri Vault
    │       └── schema.sql                                 # Script DDL di inizializzazione tabella card_tokens
    │
    └── test/java/com/aurapay/vault/
        ├── service/
        │   └── VaultServiceTest.java                      # Unit test della business logic con mock di VaultClient
        └── validator/
            └── LuhnValidatorTest.java                     # Unit test per validazione dell'algoritmo di Luhn
```

---

### Dettaglio delle Classi e delle Scelte di Design

#### 1. `VaultClient.java`
* **Integrazione HashiCorp Vault Transit**: Utilizza il motore crittografico **Transit Secrets Engine** di Vault per cifrare e decifrare i dati sensibili senza dover memorizzare o gestire chiavi crittografiche a livello applicativo.
* **Auto-Bootstrap**: Al primo avvio, il client esegue un controllo automatico sul server Vault: tenta di abilitare il mount `/v1/sys/mounts/transit` e crea la chiave crittografica `aura-pay-key` se non esiste. Questo rende l'ambiente locale completamente "zero-configuration".
* **Uso di `RestClient`**: Invece di importare la pesante suite di dipendenze di Spring Cloud Vault, si utilizza il client sincrono leggero `RestClient` (introdotto in Spring Boot 3.2), che facilita la scrittura del codice e semplifica il testing tramite `MockRestServiceServer`.

#### 2. `VaultService.java`
* **Single-Use Constraint**: Per limitare l'esposizione del PAN, i token sono monouso. Durante il recupero dei dati (`retrieve`), il campo `used_at` viene popolato e qualsiasi richiesta successiva per lo stesso token genera un'eccezione di dominio (`DOMAIN_RULE_VIOLATION`).
* **Gestione della Scadenza (TTL)**: Ogni token viene creato con una data di scadenza definita (`expires_at` impostato a 15 minuti nel futuro). Un task schedulato in background (`purgeExpiredTokens`) esegue ogni minuto una pulizia fisica eliminando dal database tutti i token scaduti.
* **Separazione degli Ambienti (Sandbox vs Live)**: Rileva automaticamente l'ambiente in base alla chiave API fornita nell'header HTTP. I token creati in ambiente test hanno il flag `is_test = true` e non possono essere utilizzati con chiavi live (e viceversa), garantendo un isolamento logico rigido.

---

* **Q: Perché usate i Java Records e non `@Data` / POJO Lombok per i DTO degli eventi?**
  * *A*: Gli eventi di dominio rappresentano fatti storici immutabili. Un Record impedisce strutturalmente la presenza di setter o la mutazione dello stato dopo l'istanziazione, garantendo thread-safety nativa e zero overhead di boilerplate senza dipendenze da plugin di compilazione terzi.
* **Q: Come prevenite i timing attacks nelle verifiche delle firme Webhook?**
  * *A*: Utilizziamo `MessageDigest.isEqual()` in `HmacUtils` per confrontare le firme generata ed attesa a tempo costante, eliminando la vulnerabilità dovuta alla terminazione anticipata dei confronti di stringhe standard.
* **Q: Perché avete scelto AES GCM invece di AES CBC per la cifratura dei dati at-rest?**
  * *A*: AES GCM fornisce Authentic Encryption (AEAD). Oltre a cifrare il dato, include un tag di autenticazione a 128 bit che protegge il payload da qualsiasi manomissione o bit-flipping prima della decifratura.

---

## 2. `aura-api-gateway` & `aura-bank-simulator` — Sessione 3

### Scopo dei Moduli
In questa sessione sono stati creati i due microservizi di ingresso e di simulazione bancaria:

1. **`aura-api-gateway` (Spring Cloud Gateway - Reactive WebFlux)**:
   * **Single Point of Entry**: Esposto sulla porta `8080`, gestisce il routing dinamico di tutte le chiamate REST dirette ai microservizi downstream (`/v1/merchants/**`, `/v1/payments/**`, `/v1/invoices/**`, `/v1/bank/**`).
   * **Distributed Tracing Header**: Genera e propaga l'header `X-Correlation-ID` su tutte le richieste in ingresso ed in uscita verso i microservizi.
   * **Global CORS & Standard Error Translation**: Configurazione centralizzata CORS per la SPA React e gestore eccezioni reattivo (`GlobalErrorWebExceptionHandler`) per risposte HTTP d'errore uniformi (`ErrorResponse`).

2. **`aura-bank-simulator` (Spring Boot 3.x REST Microservice)**:
   * **Acquiring Bank Simulator**: Esposto sulla porta `8086`, simula le risposte di autorizzazione delle reti di pagamento e di gestione rimborsi.
   * **Magic Rules Engine**: Regole deterministiche per simulare fondi insufficienti (`*99`), carta scaduta (`*98`), frode (`*97`), timeout di rete (`*95`) o approvazione immediata.
   * **Audit Event Publishing**: Pubblica l'evento `BankAuthorizationResultEvent` (da `aura-core-lib`) sul topic Kafka `aura.bank.authorization_result.v1`.

---

### Struttura dei Pacchetti e dei File

```
aura-api-gateway/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/aurapay/gateway/
    │   │   ├── AuraApiGatewayApplication.java            # Main application class
    │   │   ├── config/
    │   │   │   └── CorsConfig.java                        # Configurazione CORS Reactive WebFilter
    │   │   ├── exception/
    │   │   │   └── GlobalErrorWebExceptionHandler.java    # Exception Handler WebFlux -> ErrorResponse DTO
    │   │   └── filter/
    │   │       └── CorrelationIdFilter.java               # Global Gateway Filter per X-Correlation-ID
    │   └── resources/
    │       └── application.yml                            # Route definitions & Redis config
    └── test/
        └── java/com/aurapay/gateway/
            └── AuraApiGatewayApplicationTests.java        # Spring Cloud Gateway WebTestClient integration test

aura-bank-simulator/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/aurapay/banksimulator/
    │   │   ├── AuraBankSimulatorApplication.java         # Main application class
    │   │   ├── controller/
    │   │   │   └── BankSimulatorController.java          # Endpoint /v1/bank/authorize & /v1/bank/refund
    │   │   ├── dto/
    │   │   │   ├── BankAuthorizationRequest.java          # Request payload autorizzazione
    │   │   │   ├── BankAuthorizationResponse.java         # Response payload autorizzazione
    │   │   │   ├── BankRefundRequest.java                 # Request payload rimborso
    │   │   │   └── BankRefundResponse.java                # Response payload rimborso
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java            # RestControllerAdvice -> ErrorResponse DTO
    │   │   ├── publisher/
    │   │   │   └── BankEventPublisher.java                # Componente dedicato all'invio eventi Kafka
    │   │   ├── service/
    │   │   │   └── BankSimulatorService.java              # Magic rules engine per autorizzazioni e rimborsi
    │   │   └── util/
    │   │       └── LatencyUtils.java                      # Utility final per simulazione latenza di rete
    │   └── resources/
    │       └── application.yml                            # Port 8086, Kafka bootstrap & latency properties
    └── test/
        └── java/com/aurapay/banksimulator/
            ├── controller/
            │   └── BankSimulatorControllerTest.java       # WebMvcTest per validazione DTO ed HTTP responses
            └── service/
                └── BankSimulatorServiceTest.java          # Unit test regole deterministiche & eccezioni
```

---

### Dettaglio delle Classi e delle Scelte di Design

#### 1. `com.aurapay.gateway` (API Gateway)
* **Perché Spring Cloud Gateway (WebFlux / Netty) rispetto a Spring MVC / Tomcat?**
  * Spring Cloud Gateway si basa sul paradigm **Non-Blocking I/O (Reactive WebFlux)**.
  * In un API Gateway che instrada migliaia di chiamate I/O concorrenti verso microservizi backend, l'architettura reattiva evita il blocco dei thread del server (thread starvation), garantendo footprint di memoria minimo ed elevatissimo throughput.
* **`CorrelationIdFilter`**:
  * Implementa `GlobalFilter` e `Ordered` con priorità `HIGHEST_PRECEDENCE`.
  * Ispeziona l'header `X-Correlation-ID`. Se mancante, ne genera uno nuovo via `UUID.randomUUID().toString()`.
  * Inietta l'header sia nella richiesta in corso diretta ai microservizi downstream (`request.mutate().header(...)`), sia nella risposta HTTP diretta al client (`response.getHeaders().add(...)`).
* **`GlobalErrorWebExceptionHandler`**:
  * Implementa `ErrorWebExceptionHandler` per catturare qualsiasi eccezione generata all'interno della pipeline del Gateway (es. 404 Route Not Found, 429 Rate Limited, 503 Service Unavailable).
  * Converte lo stato HTTP ed il messaggio di errore nel formato JSON standard `ErrorResponse` di `aura-core-lib`, restituendo `application/json` con UTF-8 encoding.

#### 2. `com.aurapay.banksimulator` (Bank Simulator)
* **`BankSimulatorService`**:
  * **Algoritmo Magic Rules**: Analizza l'importo della transazione `amountCents` tramite l'operatore modulo 100 (`amountCents % 100`).
    * `99` -> Rifiutato per `INSUFFICIENT_FUNDS` (`responseCode: 51`).
    * `98` -> Rifiutato per `EXPIRED_CARD` (`responseCode: 54`).
    * `97` -> Rifiutato per `SUSPECTED_FRAUD` (`responseCode: 59`).
    * `95` -> Timeout simulato (`BusinessException` con codice `BANK_UNAVAILABLE`).
    * Qualsiasi altro importo -> Approvato (`responseCode: 00`, `authorized: true`, genera `transactionId` univoco `tx_bank_...` ed `authorizationCode` `AUTH_...`).
  * **Simulazione Latenza Netted**: Configurato tramite `${aurapay.bank.simulated-latency-ms:100}` per riprodurre in modo controllato i tempi di trasmissione di rete verso circuiti internazionali.
  * **Pubblicazione Eventi di Audit**: Ogni tentativo di autorizzazione invia un messaggio `BankAuthorizationResultEvent` su Kafka topic `aura.bank.authorization_result.v1`.
* **`GlobalExceptionHandler` (`@RestControllerAdvice`)**:
  * Intercetta `BusinessException` e `MethodArgumentNotValidException`, mappando i messaggi ed eventuali `FieldErrorDto` nel formato `ErrorResponse`.

---

### Spunti di Discussione per Colloqui / Technical Reviews
* **Q: Perché il Gateway è realizzato con Spring Cloud Gateway Reattivo anziché un normale controller Spring MVC?**
  * *A*: L'API Gateway è il bottleneck di ingresso dell'intero sistema. Usare il modello reattivo non-blocking (Netty + Reactor) consente di gestire un numero elevatissimo di connessioni HTTP simultanee mantenendo bassissimo l'uso di RAM e la CPU, senza riservare un thread per ciascuna richiesta pendente.
* **Q: Come tracciate una richiesta che attraversa molteplici microservizi partendo dal Gateway?**
  * *A*: Attraverso il `CorrelationIdFilter`, il Gateway genera (o mantiene) un header unico `X-Correlation-ID`. Tutti i microservizi trasmettono questo header nelle loro chiamate HTTP downstream e nei log (MDC), permettendo di correlare istantaneamente tutti i log appartenenti ad un singola transazione.
* **Q: Come funziona il meccanismo di Magic Numbers nel Bank Simulator?**
  * *A*: Per evitare di introdurre dipendenze da banche esterne o chiavi fittizie complesse, utilizziamo gli ultimi due numeri dell'importo in centesimi per pilotare in modo deterministico l'esito della transazione (es. 10.99€ simula fondi insufficienti, 10.95€ simula timeout). Questo approccio rende estremamente semplice ed immediata la scrittura di test automatici E2E per casi negativi ed edge-cases.

---

## 3. `aura-vault-service` (Tokenizzazione Carte) — Sessione 4

### Scopo del Modulo
`aura-vault-service` è il servizio PCI-scoped responsabile dell'isolamento dei dati di pagamento sensibili (PAN e CVV). Esso fornisce:
1. **Tokenizzazione delle Carte**: Riceve i dati della carta in chiaro, convalida formalmente il numero di carta tramite l'algoritmo di Luhn, cifra i dati sensibili delegando la cifratura a HashiCorp Vault e restituisce un token temporaneo (`tok_...`).
2. **Detokenizzazione Sicura**: Consente ai soli servizi interni autorizzati (come l'orchestratore) di recuperare i dati della carta in chiaro a partire dal token, contrassegnando immediatamente il token come utilizzato (vincolo single-use).

---

### Struttura dei Pacchetti e dei File

```
aura-vault-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/aurapay/vault/
    │   │   ├── AuraVaultApplication.java                 # Classe di ingresso Spring Boot
    │   │   │
    │   │   ├── client/
    │   │   │   └── VaultClient.java                       # Client REST per HashiCorp Vault Transit Engine
    │   │   │
    │   │   ├── controller/
    │   │   │   └── VaultController.java                   # Endpoint POST /v1/tokens e /v1/tokens/retrieve
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── TokenizeRequest.java                   # DTO di richiesta per la tokenizzazione
    │   │   │   ├── TokenResponse.java                     # DTO di risposta per la tokenizzazione
    │   │   │   ├── RetrieveRequest.java                   # DTO di richiesta per il recupero dati
    │   │   │   └── CardDetailsResponse.java               # DTO di risposta per il recupero dati
    │   │   │
    │   │   ├── entity/
    │   │   │   └── CardToken.java                         # Entità JPA per mappare il token e i metadati della carta
    │   │   │
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java            # RestControllerAdvice per la gestione uniforme degli errori
    │   │   │
    │   │   ├── repository/
    │   │   │   └── CardTokenRepository.java               # Repository per la persistenza dei token
    │   │   │
    │   │   ├── service/
    │   │   │   └── VaultService.java                      # Core business logic (Luhn, brand detection, TTL, Vault interaction)
    │   │   │
    │   │   └── validator/
    │   │       └── LuhnValidator.java                     # Algoritmo di Luhn per validazione formale del PAN
    │   │
    │   └── resources/
    │       ├── application.yml                            # Configurazione porta, DB Postgres e parametri Vault
    │       └── schema.sql                                 # Script DDL di inizializzazione tabella card_tokens
    │
    └── test/java/com/aurapay/vault/
        ├── service/
        │   └── VaultServiceTest.java                      # Unit test della business logic con mock di VaultClient
        └── validator/
            └── LuhnValidatorTest.java                     # Unit test per validazione dell'algoritmo di Luhn
```

---

### Dettaglio delle Classi e delle Scelte di Design

#### 1. `VaultClient.java`
* **Integrazione HashiCorp Vault Transit**: Utilizza il motore crittografico **Transit Secrets Engine** di Vault per cifrare e decifrare i dati sensibili senza dover memorizzare o gestire chiavi crittografiche a livello applicativo.
* **Auto-Bootstrap**: Al primo avvio, il client esegue un controllo automatico sul server Vault: tenta di abilitare il mount `/v1/sys/mounts/transit` e crea la chiave crittografica `aura-pay-key` se non esiste. Questo rende l'ambiente locale completamente "zero-configuration".
* **Uso di `RestClient`**: Invece di importare la pesante suite di dipendenze di Spring Cloud Vault, si utilizza il client sincrono leggero `RestClient` (introdotto in Spring Boot 3.2), che facilita la scrittura del codice e semplifica il testing tramite `MockRestServiceServer`.

#### 2. `VaultService.java`
* **Single-Use Constraint**: Per limitare l'esposizione del PAN, i token sono monouso. Durante il recupero dei dati (`retrieve`), il campo `used_at` viene popolato e qualsiasi richiesta successiva per lo stesso token genera un'eccezione di dominio (`DOMAIN_RULE_VIOLATION`).
* **Gestione della Scadenza (TTL)**: Ogni token viene creato con una data di scadenza definita (`expires_at` impostato a 15 minuti nel futuro). Un task schedulato in background (`purgeExpiredTokens`) esegue ogni minuto una pulizia fisica eliminando dal database tutti i token scaduti.
* **Separazione degli Ambienti (Sandbox vs Live)**: Rileva automaticamente l'ambiente in base alla chiave API fornita nell'header HTTP. I token creati in ambiente test hanno il flag `is_test = true` e non possono essere utilizzati con chiavi live (e viceversa), garantendo un isolamento logico rigido.

---

### Spunti di Discussione per Colloqui / Technical Reviews
* **Q: Perché delegare la cifratura a HashiCorp Vault invece di cifrare in Java tramite AESCryptoUtils?**
  * *A*: Delegando la cifratura a un KMS esterno (Key Management Service) come HashiCorp Vault, il microservizio non ha mai accesso alle chiavi crittografiche master in chiaro e non deve preoccuparsi della gestione sicura delle chiavi o del key versioning. Vault gestisce la crittografia in hardware (HSM) o in un ambiente isolato e protetto, riducendo drasticamente il risco di data-leak.
* **Q: Come avete implementato la conformità PCI-DSS nel servizio?**
  * *A*: Per prima cosa, abbiamo isolato i dati della carta (PAN e CVV) all'interno di questo specifico servizio. Il PAN non viene mai scritto nei log né salvato in chiaro nel database. Il database contiene solo il ciphertext restituito da Vault e un PAN mascherato (es. `453201******1111`). I token generati hanno una scadenza rapida (15 minuti) e sono monouso (vengono distrutti logici dopo il primo utilizzo).
* **Q: Come funziona il bootstrap automatico di Vault nel codice?**
  * *A*: All'avvio dell'applicazione, un metodo annotato con `@PostConstruct` in `VaultClient` interroga le API di Vault per abilitare il motore Transit (`/v1/sys/mounts/transit`) e creare la chiave di crittografia `aura-pay-key`. Se Vault è già inizializzato, l'errore di duplicato viene catturato e ignorato in modo silente, permettendo all'app di partire normalmente.

---

## 4. `aura-payment-orchestrator` (Happy Path Sincrono) — Sessione 5

### Scopo del Modulo
`aura-payment-orchestrator` è il microservizio centrale per l'orchestrazione delle transazioni di pagamento. Gestisce:
1. **Macchina a Stati del PaymentIntent**: Gestione delle transizioni di stato (`CREATED` -> `PROCESSING` -> `SUCCEEDED` / `FAILED` / `CANCELLED`).
2. **Orchestrazione Sincrona**: Coordinamento a due fasi tra `aura-vault-service` (recupero sicuro dei dati carta via token) e `aura-bank-simulator` (autorizzazione bancaria).
3. **API REST per Pagamenti**: Endpoint `/v1/payments` per creazione, conferma, ricerca e cancellazione dei pagamenti.

---

### Struttura dei Pacchetti e dei File

```
aura-payment-orchestrator/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/aurapay/orchestrator/
    │   │   ├── AuraPaymentOrchestratorApplication.java      # Classe principale Spring Boot
    │   │   │
    │   │   ├── client/
    │   │   │   ├── BankSimulatorClient.java                  # Client REST per aura-bank-simulator (/v1/bank/authorize)
    │   │   │   ├── VaultClient.java                          # Client REST per aura-vault-service (/v1/tokens/retrieve)
    │   │   │   └── dto/
    │   │   │       ├── BankAuthorizationRequest.java         # DTO richiesta autorizzazione banca
    │   │   │       ├── BankAuthorizationResponse.java        # DTO risposta autorizzazione banca
    │   │   │       ├── VaultCardDetailsResponse.java         # DTO risposta detokenizzazione carta
    │   │   │       └── VaultRetrieveRequest.java             # DTO richiesta detokenizzazione carta
    │   │   │
    │   │   ├── controller/
    │   │   │   └── PaymentController.java                    # REST Controller su /v1/payments
    │   │   │
    │   │   ├── domain/
    │   │   │   ├── PaymentIntent.java                        # Entità JPA per il tracciamento del pagamento
    │   │   │   └── enums/
    │   │   │       └── PaymentStatus.java                    # Enum stati (CREATED, PROCESSING, SUCCEEDED, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED)
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── ConfirmPaymentIntentRequest.java      # DTO richiesta conferma pagamento
    │   │   │   │   └── CreatePaymentIntentRequest.java       # DTO richiesta creazione intent
    │   │   │   └── response/
    │   │   │       └── PaymentIntentResponse.java            # DTO risposta intent con mapper statico fromEntity
    │   │   │
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java               # ControllerAdvice centralizzato -> ErrorResponse DTO
    │   │   │
    │   │   ├── repository/
    │   │   │   └── PaymentIntentRepository.java              # Spring Data JPA Repository per PaymentIntent
    │   │   │
    │   │   └── service/
    │   │       └── PaymentOrchestrationService.java          # Core Business Logic e Orchestrazione Sincrona
    │   │
    │   └── resources/
    │       └── application.yml                               # Configurazione porta 8082, PostgreSQL e URL servizi esterni
    │
    └── test/
        ├── java/com/aurapay/orchestrator/
        │   ├── AuraPaymentOrchestratorApplicationTests.java  # Integration test avvio contesto Spring Boot
        │   ├── controller/
        │   │   └── PaymentControllerTest.java                # WebMvcTest con MockitoBean e BDDMockito
        │   └── service/
        │       └── PaymentOrchestrationServiceTest.java      # Unit test logica di orchestrazione e transizioni di stato
        └── resources/
            └── application-test.yml                          # Configurazione database H2 per i test
```

---

### Dettaglio delle Classi e delle Scelte di Design

#### 1. `PaymentOrchestrationService.java`
* **Flusso di Orchestrazione Sincrona**:
  1. Riceve l'ID del `PaymentIntent` e la request di conferma contenente il `paymentMethodToken` (token carta prodotto da Vault).
  2. Verifica che lo stato sia rigorosamente `CREATED` (altrimenti lancia `DomainRuleViolationException`).
  3. Imposta lo stato su `PROCESSING` e salva a DB.
  4. Interroga `aura-vault-service` per detokenizzare la carta.
  5. Invia la richiesta di autorizzazione a `aura-bank-simulator`.
  6. Se approvata (`authorized == true`): aggiorna lo stato a `SUCCEEDED` salvando `authorizationCode` e `transactionId`.
  7. Se rifiutata (`authorized == false`): aggiorna lo stato a `FAILED` salvando il motivo di rifiuto (`failureReason`).

#### 2. `GlobalExceptionHandler.java`
* Mappa eccezioni di dominio e di rete (`ResourceNotFoundException`, `BusinessException`, `DomainRuleViolationException`, `MethodArgumentNotValidException`) nella struttura JSON standard `ErrorResponse` della `aura-core-lib`.

---

### Spunti di Discussione per Colloqui / Technical Reviews
* **Q: Come viene gestita la concorrenza e le transizioni di stato non valide sul PaymentIntent?**
  * *A*: La macchina a stati valida preventivamente lo stato corrente prima di procedere con l'orchestrazione. Se una richiesta di conferma viene inviata su un `PaymentIntent` non in stato `CREATED` (es. già `SUCCEEDED` o `FAILED`), l'orchestratore blocca la richiesta sollevando una `DomainRuleViolationException` (HTTP 422 Unprocessable Entity).
* **Q: Come si integrano gli altri microservizi durante l'orchestrazione del pagamento?**
  * *A*: L'orchestrator usa `RestClient` di Spring Boot 3.4 per effettuare chiamate HTTP sincrone verso Vault (porta 8084) per recuperare i dati della carta in modo sicuro e verso Bank Simulator (porta 8086) per l'autorizzazione.

## 5. Debezium & Outbox Pattern (Event-Driven Engine) — Sessione 6

### Scopo del Modulo
In questa sessione è stato integrato il **Transactional Outbox Pattern** nel microservizio `aura-payment-orchestrator` unitamente alla configurazione di **Debezium CDC (Change Data Capture)**:

1. **Transactional Outbox Pattern**:
   * Risolve il problema del dual-write (doppia scrittura su DB e Kafka) garantendo l'atomicità ACID: ogni cambio di stato di un `PaymentIntent` (`CREATED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`) salva nella stessa transazione DB (`@Transactional`) l'entità di dominio ed il record nella tabella `outbox_events`.
   * Il payload dell'evento outbox viene serializzato in formato JSON a partire dai Java Records strongly typed definiti in `aura-core-lib` (`PaymentIntentCreatedEvent`, `PaymentProcessingEvent`, `PaymentSucceededEvent`, `PaymentFailedEvent`).

2. **Debezium & Kafka Connect Setup**:
   * Configurazione JSON (`docker/debezium/outbox-connector.json`) per Debezium Connect in esecuzione su PostgreSQL.
   * Utilizza il Single Message Transformation (SMT) `io.debezium.transforms.outbox.EventRouter` per leggere i log di transazione WAL della tabella `outbox_events` e pubblicare in modo asincrono ed affidabile sugli specifici topic Kafka (`aura.paymentintent.created.v1`, `aura.payment.processing.v1`, `aura.payment.succeeded.v1`, `aura.payment.failed.v1`).

---

### Struttura dei Pacchetti e dei File Creati / Modificati

```
aura-payment-orchestrator/
├── src/
│   ├── main/
│   │   ├── java/com/aurapay/orchestrator/
│   │   │   ├── domain/
│   │   │   │   └── OutboxEvent.java                   # Entità JPA per la tabella outbox_events
│   │   │   ├── repository/
│   │   │   │   └── OutboxEventRepository.java         # JpaRepository per la tabella outbox_events
│   │   │   └── service/
│   │   │       ├── PaymentEventFactory.java           # Factory per la creazione e serializzazione JSON dei DTO eventi di aura-core-lib
│   │   │       └── PaymentOrchestrationService.java   # Business logic aggiornata con salvataggio atomico outbox
│   │   └── resources/
│   │       └── schema.sql                             # DDL Postgres con definizione tabella outbox_events
│   └── test/
│       └── java/com/aurapay/orchestrator/
│           └── service/
│               ├── PaymentOrchestrationServiceTest.java      # Unit test con mock di OutboxEventRepository e PaymentEventFactory
│               └── PaymentOrchestrationIntegrationTest.java # Integration test con H2 per verifica persistenza atomica outbox
docker/
└── debezium/
    └── outbox-connector.json                          # Configurazione Debezium Connect Outbox EventRouter SMT
```

---

### Dettaglio delle Classi e delle Scelte di Design

#### 1. `OutboxEvent.java` & `schema.sql`
* **Tabella `outbox_events`**:
  * `id`: UUID Primary Key
  * `aggregate_type`: `"PaymentIntent"`
  * `aggregate_id`: UUID del `PaymentIntent` (usato come partition key da Kafka per preservare l'ordinamento degli eventi del medesimo intent)
  * `event_type`: Nome del topic Kafka (es. `"aura.payment.succeeded.v1"`)
  * `payload`: Stringa JSON con il contenuto immutabile dell'evento da `aura-core-lib`
  * `created_at`: Timestamptz di creazione
  * `processed`: Boolean (default `false`) per tracciamento stato CDC

#### 2. `PaymentEventFactory.java`
* Componente Spring dedicato alla creazione degli eventi fortemente tipizzati di `aura-core-lib`:
  * Converte le entità di dominio `PaymentIntent` nei record immutable `DomainEvent` (`PaymentIntentCreatedEvent`, `PaymentProcessingEvent`, `PaymentSucceededEvent`, `PaymentFailedEvent`).
  * Converte il record event in JSON tramite `ObjectMapper` e costruisce l'entità `OutboxEvent`.

#### 3. `PaymentOrchestrationService.java`
* Garantisce che in qualsiasi transazione di business (`createPaymentIntent`, `confirmPayment`, `cancelPayment`) il salvataggio dell'entità `PaymentIntent` e la registrazione di `OutboxEvent` avvengano all'interno del medesimo contesto transazionale (`@Transactional`). Se la transazione DB fallisce o fa rollback, l'evento non viene mai scritto nella tabella outbox e di conseguenza mai pubblicato su Kafka.

---

### Spunti di Discussione per Colloqui / Technical Reviews
* **Q: Cos'è il Transactional Outbox Pattern e quale problema risolve?**
  * *A*: In un'architettura microservizi, aggiornare il database locale ed inviare direttamente un messaggio a un broker (es. Kafka) all'interno dello stesso metodo presenta il problema del dual-write. Se il DB salva ma la rete verso Kafka fallisce (o viceversa), si generano inconsistenze. Con il Transactional Outbox Pattern, l'evento viene salvato come record in una tabella `outbox_events` nello stesso database ed all'interno della medesima transazione ACID del dato di dominio. Un processo asincrono (Debezium via Change Data Capture) legge i WAL del database e pubblica l'evento su Kafka senza perdite né duplicazioni applicative.
* **Q: Come funziona il Debezium Outbox Event Router?**
  * *A*: È una trasformazione (SMT - Single Message Transformation) fornita da Debezium che estrae i campi dalla tabella `outbox_events` (id, aggregate_type, aggregate_id, event_type, payload) e li mappa direttamente sui messaggi Kafka, inviandoli al topic specificato in `event_type` ed usando `aggregate_id` come chiave di partizione.

---

## 6. Convenzioni di Codice & Standard Ecosistema


Tutti i microservizi dell'ecosistema AuraPay condividono una serie di convenzioni rigide ed immutabili garantite da audit automatici e test di regressione:

### 1. Nomenclatura e Pacchetti
- **DTO Immutabili**: Collocati in `dto.request` e `dto.response` (utilizzando Java 21 Records).
- **Entità JPA**: Segregate in `domain/`.
- **Controller & Service**: Classi con suffissi espliciti `Controller` e `Service`.

### 2. Header HTTP Centralizzati & Correlation ID
- **Header Constants**: Definiti unicamente in `AuraHeaders` (`AUTHORIZATION`, `CORRELATION_ID`, `API_KEY`).
- **Tracciamento Distribuito**: Generazione del Correlation ID sul Gateway (`CorrelationIdFilter`) ed inoltro downstream nei client HTTP via `CorrelationIdInterceptor`.

### 3. Gestione Errori Centralizzata
- Ogni microservizio espone un `@RestControllerAdvice` (`GlobalExceptionHandler`) che traduce le eccezioni di dominio e di validazione (`@Valid`) nel DTO standard `ErrorResponse` della `aura-core-lib`.

### 4. Logging & Lingua
- Uso esclusivo dell'annotazione Lombok **`@Slf4j`** per l'istanziazione dei logger.
- Messaggi di log ed eccezioni scritti unicamente in **Inglese**.

### 5. Configurazione Database & Spring Boot
- **DDL & Schema**: Inizializzazione schema via `schema.sql` autoritativo e Hibernate DDL auto in modalità `validate`.
- **Performance & Safety**: Disattivazione anti-pattern Open Session In View (`spring.jpa.open-in-view: false`) e limiti definiti per il pool HikariCP (`maximum-pool-size: 10`).

### 6. Containerizzazione & Isolamento Test
- **Docker**: `Dockerfile` multi-stage basato su `eclipse-temurin:21-jre-alpine` per tutti i microservizi.
- **Test Suite**: Annotazione `@ActiveProfiles("test")` e `@TestPropertySource` su tutti i test per isolare il database H2 dalle variabili d'ambiente OS (`SPRING_DATASOURCE_URL`).

