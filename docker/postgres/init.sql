-- Creazione dei database dedicati a ciascun microservizio per garantire l'isolamento dei dati
CREATE DATABASE aura_merchant_db;
CREATE DATABASE aura_payment_db;
CREATE DATABASE aura_orchestrator_db;
CREATE DATABASE aura_ledger_db;
CREATE DATABASE aura_invoice_db;
CREATE DATABASE aura_vault_db;
CREATE DATABASE aura_webhook_db;

-- Abilita l'accesso per l'utente aura_user su tutti i database creati
GRANT ALL PRIVILEGES ON DATABASE aura_merchant_db TO aura_user;
GRANT ALL PRIVILEGES ON DATABASE aura_payment_db TO aura_user;
GRANT ALL PRIVILEGES ON DATABASE aura_orchestrator_db TO aura_user;
GRANT ALL PRIVILEGES ON DATABASE aura_ledger_db TO aura_user;
GRANT ALL PRIVILEGES ON DATABASE aura_invoice_db TO aura_user;
GRANT ALL PRIVILEGES ON DATABASE aura_vault_db TO aura_user;
GRANT ALL PRIVILEGES ON DATABASE aura_webhook_db TO aura_user;
