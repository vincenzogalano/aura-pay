# Guida allo Studio e Spiegazione dell'Architettura dei Moduli (AuraPay)

Questo documento funge da **manuale architetturale dinamico** per lo studio e la spiegazione della codebase di AuraPay. Viene aggiornato al termine di ciascuna sessione di sviluppo verticale per riflettere la struttura dei pacchetti, le classi create, le motivazioni di design e i concetti chiave utili per interviste tecniche o code review.

---

## Indice dei Moduli
1. [aura-core-lib (Libreria Condivisa)](#1-aura-core-lib-libreria-condivisa---sessione-2)

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
    │   │
    │   ├── exception/
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
