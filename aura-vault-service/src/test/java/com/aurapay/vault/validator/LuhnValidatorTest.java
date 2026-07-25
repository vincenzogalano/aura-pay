package com.aurapay.vault.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Luhn Algorithm Validator Tests")
class LuhnValidatorTest {

    @Test
    @DisplayName("Should validate correct card numbers for major brands")
    void testValidLuhnCards() {

        assertThat(LuhnValidator.isValid("4111 1111 1111 1111")).isTrue();
        assertThat(LuhnValidator.isValid("4111-1111-1111-1111")).isTrue();
        assertThat(LuhnValidator.isValid("4111111111111111")).isTrue();

        assertThat(LuhnValidator.isValid("5105 1051 0510 5100")).isTrue();
    }

    @Test
    @DisplayName("Should decline invalid card numbers or formats")
    void testInvalidLuhnCards() {

        assertThat(LuhnValidator.isValid("4111111111111112")).isFalse();

        assertThat(LuhnValidator.isValid(null)).isFalse();
        assertThat(LuhnValidator.isValid("")).isFalse();

        assertThat(LuhnValidator.isValid("411111111111111A")).isFalse();
    }
}
