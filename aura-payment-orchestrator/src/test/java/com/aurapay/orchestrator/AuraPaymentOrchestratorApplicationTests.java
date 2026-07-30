package com.aurapay.orchestrator;

import com.aurapay.orchestrator.client.BankSimulatorClient;
import com.aurapay.orchestrator.client.VaultServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:orchestratordb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false"
})
class AuraPaymentOrchestratorApplicationTests {

    @MockitoBean
    private VaultServiceClient vaultClient;

    @MockitoBean
    private BankSimulatorClient bankSimulatorClient;

    @Test
    @DisplayName("Dovrebbe caricare il contesto applicativo Spring Boot con successo")
    void contextLoads() {
        assertNotNull(vaultClient);
        assertNotNull(bankSimulatorClient);
    }
}
