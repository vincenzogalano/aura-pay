package com.aurapay.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CardMaskingUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "4532015899001111, 453201******1111",
            "4532-0158-9900-1111, 453201******1111",
            "4532 0158 9900 1111, 453201******1111",
            "378282246310005, 378282*****0005"
    })
    @DisplayName("Should mask standard PAN maintaining BIN and Last 4")
    void shouldMaskStandardPan(String input, String expected) {
        String masked = CardMaskingUtils.maskPan(input);
        assertThat(masked).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should extract last 4 digits correctly")
    void shouldExtractLastFour() {
        assertThat(CardMaskingUtils.getLastFour("4532015899001111")).isEqualTo("1111");
        assertThat(CardMaskingUtils.getLastFour("1111")).isEqualTo("1111");
        assertThat(CardMaskingUtils.getLastFour(null)).isEqualTo("0000");
    }
}
