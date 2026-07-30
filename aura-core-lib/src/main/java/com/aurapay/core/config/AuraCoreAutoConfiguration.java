package com.aurapay.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(AuraGlobalExceptionHandler.class)
public class AuraCoreAutoConfiguration {

    @Bean
    public CorrelationIdMdcFilter correlationIdMdcFilter() {
        return new CorrelationIdMdcFilter();
    }
}
