package com.example.facesapp.unit;

import com.example.facesapp.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link PasswordUtil}.
 */
class PasswordUtilTest {

    @Test
    void hash_producesNonNullNonBlankString() {
        String hash = PasswordUtil.hash("mypassword");
        assertThat(hash).isNotBlank();
    }

    @Test
    void hash_producesDifferentHashEachTime() {
        String h1 = PasswordUtil.hash("same");
        String h2 = PasswordUtil.hash("same");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void matches_returnsTrueForCorrectPassword() {
        String hash = PasswordUtil.hash("correct");
        assertThat(PasswordUtil.matches("correct", hash)).isTrue();
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String hash = PasswordUtil.hash("correct");
        assertThat(PasswordUtil.matches("wrong", hash)).isFalse();
    }
}
