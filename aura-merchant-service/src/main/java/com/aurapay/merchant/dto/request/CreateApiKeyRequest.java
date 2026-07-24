package com.aurapay.merchant.dto.request;

import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyRequest {

    @NotNull(message = "Environment (TEST or LIVE) is required")
    private ApiKeyEnvironment environment;
}
