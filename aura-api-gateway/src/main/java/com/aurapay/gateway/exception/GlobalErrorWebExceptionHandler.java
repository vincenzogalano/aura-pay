package com.aurapay.gateway.exception;

import com.aurapay.core.constants.AuraHeaders;
import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String correlationId = exchange.getRequest().getHeaders().getFirst(AuraHeaders.CORRELATION_ID);
        if (correlationId != null && !response.getHeaders().containsKey(AuraHeaders.CORRELATION_ID)) {
            response.getHeaders().add(AuraHeaders.CORRELATION_ID, correlationId);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = AuraErrorCode.INTERNAL_SERVER_ERROR.getCode();
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred at Gateway";

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            errorCode = status.name();
            if (rse.getReason() != null) {
                message = rse.getReason();
            }
        }

        response.setStatusCode(status);
        String path = exchange.getRequest().getPath().value();

        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                errorCode,
                message,
                path
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException jpe) {
            bytes = ("{\"error\":\"" + errorCode + "\",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
