package com.aurapay.vault;

import com.aurapay.vault.client.VaultClient;
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
        "spring.datasource.url=jdbc:h2:mem:auravaultdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class AuraVaultServiceApplicationTests {

    @MockitoBean
    private VaultClient vaultClient;

    @Test
    @DisplayName("Dovrebbe caricare il contesto applicativo Spring Boot di Vault Service con successo")
    void contextLoads() {
        assertNotNull(vaultClient);
    }
}
