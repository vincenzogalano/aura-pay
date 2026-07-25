package com.aurapay.orchestrator.client;

import com.aurapay.core.constants.AuraHeaders;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get(AuraHeaders.CORRELATION_ID);
        if (correlationId != null && !request.getHeaders().containsKey(AuraHeaders.CORRELATION_ID)) {
            request.getHeaders().add(AuraHeaders.CORRELATION_ID, correlationId);
        }
        return execution.execute(request, body);
    }
}
