package com.aurapay.merchant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterMerchantResponse {

    private MerchantResponse merchant;
    private List<RawApiKeyDto> testApiKeys;
    private String message;
}
