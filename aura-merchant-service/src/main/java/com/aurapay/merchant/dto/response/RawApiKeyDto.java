package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import com.aurapay.merchant.domain.enums.ApiKeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawApiKeyDto {

    private UUID id;
    private ApiKeyEnvironment environment;
    private ApiKeyType keyType;
    private String keyPrefix;
    private String rawKey;
}
