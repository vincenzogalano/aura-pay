package com.aurapay.core.config;

import com.aurapay.core.constants.AuraHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdMdcFilterTest {

    private final CorrelationIdMdcFilter filter = new CorrelationIdMdcFilter();

    @Test
    @DisplayName("Should extract existing Correlation ID from HTTP header and populate MDC and response header")
    void shouldExtractExistingCorrelationIdHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String expectedCorrelationId = "corr-abc-123-xyz";
        request.addHeader(AuraHeaders.CORRELATION_ID, expectedCorrelationId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {
            assertThat(MDC.get(AuraHeaders.CORRELATION_ID)).isEqualTo(expectedCorrelationId);
            assertThat(MDC.get("correlationId")).isEqualTo(expectedCorrelationId);
        };

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(AuraHeaders.CORRELATION_ID)).isEqualTo(expectedCorrelationId);
        assertThat(MDC.get(AuraHeaders.CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("Should generate new Correlation ID if HTTP header is missing and clean MDC after chain execution")
    void shouldGenerateCorrelationIdWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String generatedCorrelationId = response.getHeader(AuraHeaders.CORRELATION_ID);
        assertThat(generatedCorrelationId).isNotNull().isNotBlank();
        assertThat(MDC.get(AuraHeaders.CORRELATION_ID)).isNull();
    }
}
