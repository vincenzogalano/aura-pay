package com.aurapay.gateway;

import com.aurapay.gateway.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuraApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
        assertThat(webTestClient).isNotNull();
    }

    @Test
    void whenRouteNotFound_returnsCustomErrorResponseWithCorrelationId() {
        webTestClient.get()
                .uri("/v1/non-existent-route")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().exists(CorrelationIdFilter.CORRELATION_ID_HEADER)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("NOT_FOUND")
                .jsonPath("$.path").isEqualTo("/v1/non-existent-route");
    }
}
